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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
     * Logger instance for this service.
     */
    private static final Logger LOG = LoggerFactory.getLogger(FamilyHistoryReportService.class);

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
    String sftpHost;

    /**
     * Number of threads for the async executor pool.
     * Configured via the {@code family.history.async.threads} property.
     * Defaults to 2 threads if not specified.
     */
    @ConfigProperty(name = "family.history.async.threads", defaultValue = "2")
    int asyncThreads;

    @ConfigProperty(name = "family.history.upload.psa.id", defaultValue = "1")
    int psaId;

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
     * <li>{Token} - Authentication token</li>
     * <li>{Status} - Current status</li>
     * <li>{Created} - Creation date</li>
     * </ul>
     */
    @ConfigProperty(name = "family.history.sftp.xml.template", defaultValue = """
            "<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <pdf>
                <externalSourceId>{RespondentId}</externalSourceId>
                <mrn>{Xid}</mrn>
            </pdf>
            """)
    String xmlTemplate;

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
        
        LOG.info("Family History Report Service initialized with SFTP host: {} and {} async threads", 
                sftpHost, asyncThreads);
        
        // Test SFTP connection during initialization to identify configuration issues early
        try {
            boolean connectionTest = sftpService.testConnection();
            if (connectionTest) {
                LOG.info("SFTP connection test successful during service initialization");
            } else {
                LOG.warn("SFTP connection test failed during service initialization - file uploads may fail");
            }
        } catch (Exception e) {
            LOG.error("SFTP connection test threw exception during service initialization: {}", e.getMessage(), e);
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
            LOG.info("Family History Report Service async executor shutdown completed");
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
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            // Call the transactional method to do the actual work
            try {
                doGenerateAndUploadFamilyHistoryReport(status);
            } catch (Exception e) {
                LOG.error("Failed to generate and upload family history report for respondent {}: {}",
                        status.getRespondentId(), e.getMessage(), e);
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

    @Scheduled(every = "15m")
    @Transactional
    void processUnsentUploads(){

        if (psaId != 0) {
            // Get RespondentPSA records that have failed (status = FAILED and uploadedDt is null)
            // and haven't exceeded retry limits (tries < 50)
            List<RespondentPSA> unsentPSAs = RespondentPSA.find("psaId = ?1 AND status = ?2 AND uploadedDt IS NULL AND tries < ?3",
                    psaId, "FAILED", 50).list();
            
            LOG.info("Found {} unsent uploads to retry", unsentPSAs.size());
            
            for (RespondentPSA respondentPSA : unsentPSAs) {
                try {
                    LOG.info("Retrying upload for respondent {} (attempt {})", 
                            respondentPSA.respondentId, respondentPSA.tries + 1);
                    
                    // Get the status for this respondent
                    Status status = Status.find("respondentId = ?1", respondentPSA.respondentId).firstResult();
                    if (status != null) {
                        // Try the upload again
                        doGenerateAndUploadFamilyHistoryReport(status);
                        // Update the RespondentPSA with no error
                        updateRespondentPSAStatus(respondentPSA.respondentId, null);
                    } else {
                        LOG.warn("No status record found for respondent {}, skipping retry", respondentPSA.respondentId);
                    }
                } catch (Exception e) {                    
                    // Update the RespondentPSA with the new failure
                    updateRespondentPSAStatus(respondentPSA.respondentId, e);
                }
            }
        } else {
            LOG.warn("No Post Survey Action found with name 'Generate Family History Report'");
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
        LOG.info("Generating family history report for respondent {} with external ID {}",
                status.getRespondentId(), status.getXid());

        // Generate PDF report
        LOG.info("Starting PDF generation for respondent: {}", status.getRespondentId());
        byte[] pdfData = generateFamilyHistoryPdf(status.getRespondentId());
        String pdfFileName = status.getXid() + ".pdf";
        LOG.info("PDF generated successfully: {} ({} bytes)", pdfFileName, pdfData.length);

        // Generate XML metadata
        LOG.info("Generating XML metadata for respondent: {}", status.getRespondentId());
        String xmlMetadata = generateXmlMetadata(status);
        String xmlFileName = status.getXid() + "-index.xml";
        LOG.info("XML metadata generated successfully: {} ({} bytes)", xmlFileName, xmlMetadata.length());


        // Upload files to SFTP server
        LOG.info("Starting SFTP upload for files: {} and {}", pdfFileName, xmlFileName);
        try {
            LOG.info("Uploading PDF file: {}", pdfFileName);
            sftpService.uploadFile(pdfFileName, pdfData);
            LOG.info("PDF file uploaded successfully: {}", pdfFileName);
        } catch (Exception e) {
            LOG.error("Failed to upload PDF file {}: {}", pdfFileName, e.getMessage(), e);
            throw e;
        }
        try {
            LOG.info("Uploading XML file: {}", xmlFileName);
            sftpService.uploadFile(xmlFileName, xmlMetadata.getBytes(StandardCharsets.UTF_8));
            LOG.info("XML file uploaded successfully: {}", xmlFileName);
            LOG.info("XML content: {}", xmlMetadata);
        } catch (Exception e) {
            LOG.error("Failed to upload XML file {}: {}", xmlFileName, e.getMessage(), e);
            throw e;
        }
        LOG.info("Successfully uploaded family history report files for external ID: {}", status.getXid());
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
            if (this.psaId == 0) {
                LOG.warn("Skipping RespondentPSA status update for respondent {} - invalid psaId: {}", 
                        respondentId, psaId);
                return;
            }
            
            // Verify the post-survey action exists before creating RespondentPSA record
            Long psaCount = PostSurveyAction.count("id = ?1", psaId);
            if (psaCount == 0) {
                LOG.warn("Skipping RespondentPSA status update for respondent {} - " +
                        "post-survey action {} does not exist", respondentId, psaId);
                return;
            }
            
            // Find or create RespondentPSA record to track execution status
            RespondentPSA respondentPSA = RespondentPSA.find("respondentId = ?1 and psaId = ?2", 
                    respondentId, psaId).firstResult();
            
            if (respondentPSA == null) {
                respondentPSA = new RespondentPSA();
                respondentPSA.respondentId = respondentId;
                respondentPSA.psaId = psaId;
                respondentPSA.status = "STARTED";
                LOG.debug("Created new RespondentPSA record for respondent {} and PSA {}", 
                        respondentId, psaId);
            }
            
            if (throwable != null) {
                LOG.error("CompletableFuture completed exceptionally for respondent {}: {}", 
                        respondentId, throwable.getMessage(), throwable);
                        
                // Update RespondentPSA with error status
                respondentPSA.status = "FAILED";
                respondentPSA.error = throwable.getMessage();

            } else {
                LOG.info("CompletableFuture completed successfully for respondent {}", respondentId);
                
                // Update RespondentPSA with success status
                respondentPSA.status = "COMPLETED";
                respondentPSA.error = null;
                respondentPSA.uploadedDt = OffsetDateTime.now();
            }
            // Increment the tries value
            respondentPSA.tries = respondentPSA.tries + 1;
            respondentPSA.persist();
            LOG.debug("Updated RespondentPSA status to {} for respondent {} and PSA {}", 
                    respondentPSA.status, respondentId, psaId);
            
        } catch (Exception e) {
            LOG.error("Failed to update RespondentPSA status for respondent {} and PSA {}: {}", 
                    respondentId, psaId, e.getMessage(), e);
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
        LOG.debug("Generating PDF for respondent: {}", respondentId);

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
     * <p>The method performs string replacement for all supported placeholders
     * defined in the XML template. Each placeholder is replaced with the
     * corresponding value from the status object.</p>
     * 
     * @param status the status record containing all the data needed for
     *               placeholder substitution in the XML template
     * @return a string containing the generated XML metadata with all
     *         placeholders replaced with actual values
     * @see #xmlTemplate
     */
    private String generateXmlMetadata(Status status) {
        LOG.debug("Generating XML metadata for respondent: {} with external ID: {}", status);

        String xmlDoc = xmlTemplate;
        // Any value in the status document can be included in the xml.
        // Use null-safe replacements to handle missing/null values
        xmlDoc = xmlDoc.replace("{Xid}", nullSafe(status.getXid()));
        xmlDoc = xmlDoc.replace("{RespondentId}", Long.toString(status.getRespondentId()));
        xmlDoc = xmlDoc.replace("{SurveyId}", Long.toString(status.getSurveyId()));
        xmlDoc = xmlDoc.replace("{FirstName}", nullSafe(status.getFirstName()));
        xmlDoc = xmlDoc.replace("{LastName}", nullSafe(status.getLastName()));
        xmlDoc = xmlDoc.replace("{MiddleName}", nullSafe(status.getMiddleName()));
        xmlDoc = xmlDoc.replace("{Dob}", status.getDob() != null ? status.getDob().toString() : "");
        xmlDoc = xmlDoc.replace("{Email}", nullSafe(status.getEmail()));
        xmlDoc = xmlDoc.replace("{Phone}", nullSafe(status.getPhone()));
        xmlDoc = xmlDoc.replace("{DepartmentName}", nullSafe(status.getDepartmentName()));
        xmlDoc = xmlDoc.replace("{DepartmentID}", Long.toString(status.getDepartmentId()));
        xmlDoc = xmlDoc.replace("{Token}", nullSafe(status.getToken()));
        xmlDoc = xmlDoc.replace("{Status}", nullSafe(status.getStatus()));
        xmlDoc = xmlDoc.replace("{Created}", nullSafe(status.getCreated()));
        xmlDoc = xmlDoc.replace("{Finalized}", nullSafe(status.getFinalized()));
        return xmlDoc;
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
