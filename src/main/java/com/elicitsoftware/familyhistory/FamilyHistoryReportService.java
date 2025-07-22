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

import com.elicitsoftware.model.Status;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

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
     * Logger instance for this service.
     */
    private static final Logger LOG = LoggerFactory.getLogger(FamilyHistoryReportService.class);

    /**
     * SFTP server hostname or IP address for file uploads.
     * Configured via the {@code family.history.sftp.host} property.
     */
    @ConfigProperty(name = "family.history.sftp.host")
    String sftpHost;

    /**
     * Username for SFTP server authentication.
     * Configured via the {@code family.history.sftp.username} property.
     */
    @ConfigProperty(name = "family.history.sftp.username")
    String sftpUsername;

    /**
     * Password for SFTP server authentication.
     * Configured via the {@code family.history.sftp.password} property.
     */
    @ConfigProperty(name = "family.history.sftp.password")
    String sftpPassword;

    /**
     * Remote path on the SFTP server where files will be uploaded.
     * Configured via the {@code family.history.sftp.path} property.
     */
    @ConfigProperty(name = "family.history.sftp.path")
    String sftpPath;

    /**
     * SFTP server port number.
     * Configured via the {@code family.history.sftp.port} property.
     * Defaults to port 22 if not specified.
     */
    @ConfigProperty(name = "family.history.sftp.port", defaultValue = "22")
    int sftpPort;

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
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <umhsdoc2>
                <imageSource>MiGHT</imageSource>
                <updateFlag>N</updateFlag>
                <externalSourceId>{RespondentId}</externalSourceId>
                <mrn>{Xid}</mrn>
                <formDesignatorNum>38670</formDesignatorNum>
                <docDate>{Finalized}</docDate>
                <pages>
                    <page>{RespondentId}.pdf</page>
                </pages>
            </umhsdoc2>
            """)
    String xmlTemplate;

    /**
     * Injected SFTP service for handling file uploads to the remote server.
     */
    @Inject
    SftpService sftpService;

    /**
     * Injected PDF service for generating family history reports in PDF format.
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
        LOG.info("Family History Report Service initialized with SFTP host: {}", sftpHost);
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
     * <p>The operation runs asynchronously using {@link CompletableFuture} to avoid
     * blocking the calling thread. File names are derived from the external ID (Xid)
     * in the status record.</p>
     * 
     * <p>The method is transactional to ensure data consistency during the process.</p>
     * 
     * @param status the status record containing respondent information and metadata
     *               required for report generation and file naming
     * @return a {@link CompletableFuture} that completes when the operation finishes
     *         successfully or throws a RuntimeException if an error occurs
     * @throws RuntimeException if report generation or file upload fails
     * @see #generateFamilyHistoryPdf(Long)
     * @see #generateXmlMetadata(Status)
     * @see SftpService#uploadFile(String, byte[])
     */
    @Transactional
    public CompletableFuture<Void> generateAndUploadFamilyHistoryReport(Status status) {
        return CompletableFuture.runAsync(() -> {
            try {
                LOG.info("Generating family history report for respondent {} with external ID {}",
                        status.getRespondentId(), status.getXid());

                // Generate PDF report
                byte[] pdfData = generateFamilyHistoryPdf(status.getRespondentId());
                String pdfFileName = status.getXid() + ".pdf";

                // Generate XML metadata
                String xmlMetadata = generateXmlMetadata(status);
                String xmlFileName = status.getXid() + ".xml";

                // Upload files to SFTP server
                sftpService.uploadFile(pdfFileName, pdfData);
                sftpService.uploadFile(xmlFileName, xmlMetadata.getBytes(StandardCharsets.UTF_8));

                LOG.info("Successfully uploaded family history report files for external ID: {}", status.getXid());

            } catch (Exception e) {
                LOG.error("Failed to generate and upload family history report for respondent {}: {}",
                        status.getRespondentId(), e.getMessage(), e);
                throw new RuntimeException("Failed to process family history report", e);
            }
        });
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
     * @see PDFService#generatePDF(Long)
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
        xmlDoc = xmlDoc.replace("{Xid}", status.getXid());
        xmlDoc = xmlDoc.replace("{RespondentId}", Long.toString(status.getRespondentId()));
        xmlDoc = xmlDoc.replace("{SurveyId}", Long.toString(status.getSurveyId()));
        xmlDoc = xmlDoc.replace("{FirstName}", status.getFirstName());
        xmlDoc = xmlDoc.replace("{LastName}", status.getLastName());
        xmlDoc = xmlDoc.replace("{MiddleName}", status.getMiddleName());
        xmlDoc = xmlDoc.replace("{Dob}", status.getDob().toString());
        xmlDoc = xmlDoc.replace("{Email}", status.getEmail());
        xmlDoc = xmlDoc.replace("{Phone}", status.getPhone());
        xmlDoc = xmlDoc.replace("{DepartmentName}", status.getDepartmentName());
        xmlDoc = xmlDoc.replace("{DepartmentID}", Long.toString(status.getDepartmentId()));
        xmlDoc = xmlDoc.replace("{Token", status.getToken());
        xmlDoc = xmlDoc.replace("{Status}", status.getStatus());
        xmlDoc = xmlDoc.replace("{Created}", status.getCreated());
        xmlDoc = xmlDoc.replace("{Finalized}", status.getFinalized());
        return xmlDoc;
    }
}
