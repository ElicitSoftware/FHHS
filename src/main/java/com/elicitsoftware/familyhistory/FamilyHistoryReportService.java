package com.elicitsoftware.familyhistory;

/*-
 * ***LICENSE_START***
 * Elicit FHHS
 * %%
 * Copyright (C) 2025 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import com.elicitsoftware.model.PostSurveyAction;
import com.elicitsoftware.model.RespondentPSA;
import com.elicitsoftware.model.Status;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.quarkus.logging.Log;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for generating family history reports as PDF and XML metadata files
 * and uploading them to an SFTP server.
 * 
 * <p>This service creates comprehensive family history reports in PDF format
 * and accompanying XML metadata files, then uploads both files to a configured
 * SFTP server for external processing or archival. The service operates
 * asynchronously to avoid blocking the calling thread during report generation
 * and file upload operations.</p>
 * 
 * <p>The service is responsible for:</p>
 * <ul>
 * <li>Generating PDF reports containing family history data</li>
 * <li>Creating XML metadata files with document information</li>
 * <li>Uploading both files to an SFTP server</li>
 * <li>Managing request context for proper bean lifecycle</li>
 * </ul>
 * 
 * <p>Configuration is handled through MicroProfile Config properties for
 * SFTP connection details and XML template customization.</p>
 * 
 * @author Elicit Software
 * @version 1.0
 * @since 1.0
 * @see SftpService
 * @see PDFService
 * @see Status
 */
@ApplicationScoped
public class FamilyHistoryReportService {

    /**
     * Default constructor for CDI injection.
     */
    public FamilyHistoryReportService() {
        // Default constructor for CDI
    }

    /**
     * Dedicated executor service for async report generation tasks.
     * This ensures reliable async execution in Docker containers where
     * the common ForkJoinPool might not work properly.
     */
    private ExecutorService asyncExecutor;

    /**
     * SFTP server hostname or IP address for file uploads.
     * Configured via the {@code family.history.sftp.host} property.
     */
    @ConfigProperty(name = "family.history.sftp.host")
    Optional<String> sftpHost;

    /**
     * Whether the SFTP upload feature is enabled.
     * Configured via the {@code family.history.sftp.enabled} property.
     * Defaults to {@code false}: a deployment opts in to SFTP delivery once it
     * has an SFTP target to point at. When {@code false}, no connection test,
     * report generation/upload, or retry processing is performed.
     */
    @ConfigProperty(name = "family.history.sftp.enabled", defaultValue = "false")
    boolean sftpEnabled;

    /**
     * Number of threads for the async executor pool.
     * Configured via the {@code family.history.async.threads} property.
     * Defaults to 2 threads if not specified.
     */
    @ConfigProperty(name = "family.history.async.threads", defaultValue = "2")
    int asyncThreads;

    /**
     * The post-survey action FHHS records its uploads against, found by its key
     * ({@code family.history.upload.psa.key}) because its id is whatever the import minted
     * (UC-005 BR-004). {@code family.history.upload.psa.id}, if set, overrides the lookup.
     */
    @ConfigProperty(name = "family.history.upload.psa.key")
    String psaKey;

    /** An explicit id, for a deployment that prefers to pin it; wins over the key. */
    @ConfigProperty(name = "family.history.upload.psa.id")
    Optional<Integer> configuredPsaId;

    /** The resolved id, remembered once found; {@code -1} until then. */
    private volatile int resolvedPsaId = -1;

    /**
     * The id of the post-survey action to record against, or {@code 0} when it cannot be
     * found -- in which case no execution record is written, as before (UC-004).
     */
    int psaId() {
        if (configuredPsaId.isPresent()) {
            return configuredPsaId.get();
        }
        if (resolvedPsaId < 0) {
            PostSurveyAction action = PostSurveyAction.find("postSurveyActionKey = ?1", UUID.fromString(psaKey)).firstResult();
            if (action == null) {
                // Not remembered: the survey (and its action) may be imported later.
                return 0;
            }
            resolvedPsaId = action.id;
        }
        return resolvedPsaId;
    }

    /**
     * XML template used for generating metadata files.
     * The template supports placeholder substitution using curly braces.
     * Configured via the {@code family.history.sftp.xml.template} property.
     * 
     * <p>Supported placeholders include:</p>
     * <ul>
     * <li>{RespondentId} - The respondent's unique identifier</li>
     * <li>{Xid} - The external identifier</li>
     * <li>{Finalized} - The finalization date</li>
     * <li>{FirstName}, {LastName}, {MiddleName} - Name components</li>
     * <li>{Dob} - Date of birth</li>
     * <li>{Email}, {Phone} - Contact information</li>
     * <li>{DepartmentName}, {DepartmentID} - Department details</li>
     * <li>{SurveyId} - Survey identifier</li>
     * <li>{AccessCode} - The respondent's survey access code</li>
     * <li>{Status} - Current status</li>
     * <li>{Created} - Creation date</li>
     * </ul>
     */
    @ConfigProperty(name = "family.history.sftp.xml.template", defaultValue = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <pdf>
                <externalSourceId>{RespondentId}</externalSourceId>
                <mrn>{Xid}</mrn>
            </pdf>
            """)
    String xmlTemplate;

    /** UC-004 BR-005: a placeholder in the XML template, such as {@code {Xid}}. */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\w+)\\}");

    /** UC-004 BR-007: the error recorded when the XID cannot name the delivery files. */
    static final String INVALID_XID_MESSAGE =
            "The respondent's external ID is missing or cannot be used as a file name";

    /**
     * Injected SFTP service for file upload operations.
     */
    @Inject
    SftpService sftpService;

    /**
     * Injected PDF service for report generation.
     */
    @Inject
    PDFService pdfService;
    
    /**
     * Initializes the service and logs configuration information.
     * This method is called automatically after dependency injection is complete.
     * 
     * @see PostConstruct
     */
    @PostConstruct
    void init() {
        // Initialize dedicated executor for async tasks to ensure reliable execution in Docker
        this.asyncExecutor = Executors.newFixedThreadPool(asyncThreads, r -> {
            Thread t = new Thread(r, "family-history-async-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
        
        Log.infov("Family History Report Service initialized with SFTP host: {0} and {1} async threads (SFTP enabled: {2})",
                sftpHost.orElse("<not configured>"), asyncThreads, sftpEnabled);

        if (!sftpEnabled) {
            Log.info("SFTP upload is disabled via family.history.sftp.enabled=false - skipping connection test");
            return;
        }

        // Test SFTP connection during initialization to identify configuration issues early
        try {
            boolean connectionTest = sftpService.testConnection();
            if (connectionTest) {
                Log.info("SFTP connection test successful during service initialization");
            } else {
                Log.warn("SFTP connection test failed during service initialization - file uploads may fail");
            }
        } catch (Exception e) {
            Log.errorv(e, "SFTP connection test threw exception during service initialization: {0}", e.getMessage());
        }
    }

    /**
     * Cleanup method called when the application shuts down.
     * Properly shuts down the executor service.
     */
    @PreDestroy
    void destroy() {
        if (asyncExecutor != null && !asyncExecutor.isShutdown()) {
            asyncExecutor.shutdown();
            Log.info("Family History Report Service async executor shutdown completed");
        }
    }

        /**
     * Generates and uploads a family history report asynchronously.
     * 
     * <p>This method orchestrates the complete report generation and upload process:</p>
     * <ol>
     * <li>Generates a PDF report containing family history data</li>
     * <li>Creates an XML metadata file with document information</li>
     * <li>Uploads both files to the configured SFTP server</li>
     * </ol>
     * 
     * <p>The operation runs asynchronously using a dedicated {@link ExecutorService} to ensure
     * reliable execution in Docker containers where the default ForkJoinPool might not work properly.
     * File names are derived from the external ID (Xid) in the status record.</p>
     * 
     * <p>The method is transactional to ensure data consistency during the process.</p>
     * 
     * @param status the status record containing respondent information and metadata
     *               required for report generation and file naming
     * @return a {@link CompletableFuture} that completes when the operation finishes
     * @throws RuntimeException if report generation or upload fails
     * @see #generateFamilyHistoryPdf(Long)
     * @see #generateXmlMetadata(Status)
     * @see SftpService#uploadFile(String, byte[])
     */
    public CompletableFuture<Void> generateAndUploadFamilyHistoryReport(Status status) {
        if (!sftpEnabled) {
            Log.infov("SFTP upload is disabled via family.history.sftp.enabled=false - skipping report generation for respondent {0}",
                    status.getRespondentId());
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            // Call the transactional method to do the actual work
            try {
                doGenerateAndUploadFamilyHistoryReport(status);
            } catch (Exception e) {
                Log.errorv(e, "Failed to generate and upload family history report for respondent {0}: {1}",
                        status.getRespondentId(), e.getMessage());
                throw new RuntimeException("Failed to process family history report", e);
            }
        }, asyncExecutor); // Use dedicated executor instead of default ForkJoinPool
        
        // Add exception handling to catch any issues with the CompletableFuture
        future.whenComplete((result, throwable) -> {
            // Update RespondentPSA status using a separate transactional method
            // since this callback executes outside the main transaction context
            updateRespondentPSAStatus(status.getRespondentId(), throwable);
        });
        
        return future;
    }

    /**
     * Scheduled task that processes failed uploads and retries them.
     * Runs every 15 minutes to check for RespondentPSA records with FAILED status
     * that haven't been uploaded yet and haven't exceeded retry limits.
     */
    @Scheduled(every = "15m")
    @Transactional
    void processUnsentUploads(){

        if (!sftpEnabled) {
            Log.debug("SFTP upload is disabled via family.history.sftp.enabled=false - skipping retry processing");
            return;
        }

        if (psaId() != 0) {
            // Get RespondentPSA records that have failed (status = FAILED and uploadedDt is null)
            // and haven't exceeded retry limits (tries < 50)
            List<RespondentPSA> unsentPSAs = RespondentPSA.find("psaId = ?1 AND status = ?2 AND uploadedDt IS NULL AND tries < ?3",
                    psaId(), "FAILED", 50).list();
            
            Log.infov("Found {0} unsent uploads to retry", unsentPSAs.size());

            for (RespondentPSA respondentPSA : unsentPSAs) {
                try {
                    Log.infov("Retrying upload for respondent {0} (attempt {1})",
                            respondentPSA.respondentId, respondentPSA.tries + 1);
                    
                    // Get the status for this respondent
                    Status status = Status.find("respondentId = ?1", respondentPSA.respondentId).firstResult();
                    if (status != null) {
                        // Try the upload again
                        doGenerateAndUploadFamilyHistoryReport(status);
                        // Update the RespondentPSA with no error
                        updateRespondentPSAStatus(respondentPSA.respondentId, null);
                    } else {
                        Log.warnv("No status record found for respondent {0}, skipping retry", respondentPSA.respondentId);
                    }
                } catch (Exception e) {
                    // Update the RespondentPSA with the new failure
                    updateRespondentPSAStatus(respondentPSA.respondentId, e);
                }
            }
        } else {
            Log.warn("No post-survey action found for family.history.upload.psa.key; failed uploads are not retried");
        }
    }

    /**
     * Performs the actual report generation and upload work within a transaction.
     * This method is called from the async executor to ensure proper transaction handling.
     * 
     * @param status the status record containing respondent information
     * @throws Exception if report generation or upload fails
     */
    @Transactional
    public void doGenerateAndUploadFamilyHistoryReport(Status status) throws Exception {
        long respondentId = status.getRespondentId();

        // UC-004 BR-007: an XID that cannot name a file fails here, before any work is done.
        String baseName = deliveryBaseName(status.getXid());

        // UC-004 BR-008: records below name the respondent ID, the kind of file and its size.
        Log.infov("Generating family history report for respondent {0}", respondentId);

        // Generate PDF report
        Log.infov("Starting PDF generation for respondent: {0}", respondentId);
        byte[] pdfData = generateFamilyHistoryPdf(respondentId);
        String pdfFileName = baseName + ".pdf";
        Log.infov("PDF generated successfully for respondent {0} ({1} bytes)", respondentId, pdfData.length);

        // Generate XML metadata
        Log.infov("Generating XML metadata for respondent: {0}", respondentId);
        String xmlMetadata = generateXmlMetadata(status);
        String xmlFileName = baseName + "-index.xml";
        Log.infov("XML metadata generated successfully for respondent {0} ({1} characters)",
                respondentId, xmlMetadata.length());


        // Upload files to SFTP server
        Log.infov("Starting SFTP upload of the PDF and XML metadata for respondent {0}", respondentId);
        try {
            sftpService.uploadFile(pdfFileName, pdfData);
            Log.infov("PDF file uploaded successfully for respondent {0}", respondentId);
        } catch (Exception e) {
            Log.errorv(e, "Failed to upload the PDF file for respondent {0}: {1}", respondentId, e.getMessage());
            throw e;
        }
        try {
            sftpService.uploadFile(xmlFileName, xmlMetadata.getBytes(StandardCharsets.UTF_8));
            Log.infov("XML metadata file uploaded successfully for respondent {0}", respondentId);
        } catch (Exception e) {
            Log.errorv(e, "Failed to upload the XML metadata file for respondent {0}: {1}", respondentId, e.getMessage());
            throw e;
        }
        Log.infov("Successfully uploaded family history report files for respondent {0}", respondentId);
    }

    /**
     * UC-004 BR-007: the name both delivery files are built from. A usable XID is returned exactly
     * as it is stored, so the file names of an ordinary XID do not change.
     *
     * @param xid the respondent's external ID
     * @return the XID, unchanged
     * @throws IllegalStateException if the XID is missing or blank, is {@code .} or {@code ..}, or
     *                               contains a path separator or a control character; the message
     *                               names the rule and never the value
     */
    static String deliveryBaseName(String xid) {
        if (xid == null || xid.isBlank() || !SftpService.isPlainFileName(xid)) {
            throw new IllegalStateException(INVALID_XID_MESSAGE);
        }
        return xid;
    }

    /**
     * Updates the RespondentPSA status tracking record in a separate transaction.
     * 
     * <p>This method is called asynchronously from the CompletableFuture completion
     * callback to update the execution status of a post-survey action. It runs in
     * its own transaction context to ensure proper database operations.</p>
     * 
     * <p>The method will only create/update RespondentPSA records if the psaId
     * corresponds to a valid post-survey action to avoid foreign key constraint
     * violations.</p>
     * 
     * @param respondentId the respondent ID
     * @param throwable the exception if the operation failed, null if successful
     */
    @Transactional
    public void updateRespondentPSAStatus(Long respondentId, Throwable throwable) {
        try {
            // Skip if psaId is null or 0 to avoid foreign key constraint violations
            if (psaId() == 0) {
                Log.warnv("Skipping RespondentPSA status update for respondent {0} - invalid psaId: {1}",
                        respondentId, psaId());
                return;
            }

            // Verify the post-survey action exists before creating RespondentPSA record
            Long psaCount = PostSurveyAction.count("id = ?1", psaId());
            if (psaCount == 0) {
                Log.warnv("Skipping RespondentPSA status update for respondent {0} - " +
                        "post-survey action {1} does not exist", respondentId, psaId());
                return;
            }

            // Find or create RespondentPSA record to track execution status
            RespondentPSA respondentPSA = RespondentPSA.find("respondentId = ?1 and psaId = ?2",
                    respondentId, psaId()).firstResult();

            if (respondentPSA == null) {
                respondentPSA = new RespondentPSA();
                respondentPSA.respondentId = respondentId;
                respondentPSA.psaId = psaId();
                respondentPSA.status = "STARTED";
                Log.debugv("Created new RespondentPSA record for respondent {0} and PSA {1}",
                        respondentId, psaId());
            }

            if (throwable != null) {
                Log.errorv(throwable, "CompletableFuture completed exceptionally for respondent {0}: {1}",
                        respondentId, throwable.getMessage());

                // Update RespondentPSA with error status
                respondentPSA.status = "FAILED";
                respondentPSA.error = throwable.getMessage();

            } else {
                Log.infov("CompletableFuture completed successfully for respondent {0}", respondentId);

                // Update RespondentPSA with success status
                respondentPSA.status = "COMPLETED";
                respondentPSA.error = null;
                respondentPSA.uploadedDt = OffsetDateTime.now();
            }
            // Increment the tries value
            respondentPSA.tries = respondentPSA.tries + 1;
            respondentPSA.persist();
            Log.debugv("Updated RespondentPSA status to {0} for respondent {1} and PSA {2}",
                    respondentPSA.status, respondentId, psaId());

        } catch (Exception e) {
            Log.errorv(e, "Failed to update RespondentPSA status for respondent {0} and PSA {1}: {2}",
                    respondentId, psaId(), e.getMessage());
            // Don't rethrow - this is a status tracking operation that shouldn't fail the main process
        }
    }

    /**
     * Generates a PDF report containing comprehensive family history data.
     * 
     * <p>This method handles the generation of a PDF document containing all
     * relevant family history information for the specified respondent. It
     * manages the request context lifecycle to ensure proper operation with
     * request-scoped beans.</p>
     * 
     * <p>The method checks if a request context is already active and activates
     * one if necessary, ensuring that request-scoped dependencies can be properly
     * injected and used during PDF generation.</p>
     *
     * @param respondentId the respondent's unique identifier used to retrieve
     *                     family history data for the report
     * @return byte array containing the generated PDF data
     * @throws RuntimeException if PDF generation fails or respondent data cannot be retrieved
     * @see PDFService#generatePDF(long)
     * @see ManagedContext
     */
    @Transactional
    public byte[] generateFamilyHistoryPdf(Long respondentId) {
        Log.debugv("Generating PDF for respondent: {0}", respondentId);

        // Activate request context for calling request-scoped beans
        ManagedContext requestContext = Arc.container().requestContext();

        if (requestContext.isActive()) {
            // Request context is already active, proceed normally
            return pdfService.generatePDF(respondentId);
        } else {
            // Activate request context programmatically
            requestContext.activate();
            try {
                return pdfService.generatePDF(respondentId);
            } finally {
                requestContext.terminate();
            }
        }
    }

    /**
     * Generates XML metadata for a family history report.
     * 
     * <p>This method creates an XML metadata file by substituting placeholders
     * in the configured XML template with actual values from the status record.
     * The resulting XML contains document metadata that accompanies the PDF
     * report when uploaded to the SFTP server.</p>
     * 
     * <p>The method replaces all supported placeholders defined in the XML template
     * in a single pass. Each placeholder is replaced with the corresponding value
     * from the status object.</p>
     * 
     * @param status the status record containing all the data needed for
     *               placeholder substitution in the XML template
     * @return a string containing the generated XML metadata with all
     *         placeholders replaced with actual values
     * @see #xmlTemplate
     */
    private String generateXmlMetadata(Status status) {
        Log.debugv("Generating XML metadata for respondent: {0}", status.getRespondentId());

        // Any value in the status document can be included in the xml.
        // Use null-safe values to handle missing/null values
        Map<String, String> values = new HashMap<>();
        values.put("Xid", nullSafe(status.getXid()));
        values.put("RespondentId", Long.toString(status.getRespondentId()));
        values.put("SurveyId", Long.toString(status.getSurveyId()));
        values.put("FirstName", nullSafe(status.getFirstName()));
        values.put("LastName", nullSafe(status.getLastName()));
        values.put("MiddleName", nullSafe(status.getMiddleName()));
        values.put("Dob", status.getDob() != null ? status.getDob().toString() : "");
        values.put("Email", nullSafe(status.getEmail()));
        values.put("Phone", nullSafe(status.getPhone()));
        values.put("DepartmentName", nullSafe(status.getDepartmentName()));
        values.put("DepartmentID", Long.toString(status.getDepartmentId()));
        values.put("AccessCode", nullSafe(status.getAccessCode()));
        values.put("Status", nullSafe(status.getStatus()));
        values.put("Created", nullSafe(status.getCreated()));
        values.put("Finalized", nullSafe(status.getFinalized()));

        // UC-004 BR-005: one pass over the template. A value is placed as it is and is never read
        // again, so placeholder text inside a value stays literal; an unknown placeholder is kept.
        return PLACEHOLDER.matcher(xmlTemplate).replaceAll(match -> {
            String value = values.get(match.group(1));
            return Matcher.quoteReplacement(value != null ? value : match.group());
        });
    }
    
    /**
     * Helper method to safely handle null values during string replacement.
     * 
     * @param value the value to check for null
     * @return the original value if not null, empty string otherwise
     */
    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}
