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
import com.elicitsoftware.request.ReportRequest;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST service for handling family history report generation and upload requests.
 * 
 * <p>This service provides endpoints that can be called by post-survey actions
 * to generate and upload family history reports after a survey is completed.
 * The service handles asynchronous report generation and provides health monitoring
 * capabilities.</p>
 * 
 * <p>The service is designed to be used as part of the Family Health History System (FHHS)
 * workflow, specifically for processing completed family history surveys and generating
 * reports that can be uploaded to external systems.</p>
 * 
 * @author Elicit Software
 * @version 1.0
 * @since 1.0
 * @see FamilyHistoryReportService
 * @see ReportRequest
 */
@Path("/familyhistory")
@RequestScoped
public class FamilyHistoryService {

    /**
     * Default constructor for CDI injection.
     */
    public FamilyHistoryService() {
        // Default constructor for CDI
    }

    /**
     * The logger instance for this service.
     */
    private static final Logger LOG = LoggerFactory.getLogger(FamilyHistoryService.class);
    
    /**
     * Injected service for handling family history report generation and upload operations.
     */
    @Inject
    FamilyHistoryReportService reportService;

    /**
     * Endpoint for generating and uploading a family history report.
     * 
     * <p>This endpoint is designed to be called by post-survey actions
     * after a family history survey is completed. It validates the incoming
     * request, retrieves the associated status record, and initiates
     * asynchronous report generation and upload.</p>
     * 
     * <p>The operation is transactional to ensure data consistency during
     * the report generation process initiation.</p>
     * 
     * @param request the report generation request containing the respondent ID
     *                and other necessary information for report generation
     * @return HTTP response indicating success or failure:
     *         <ul>
     *         <li>200 OK - Report generation successfully initiated</li>
     *         <li>400 Bad Request - Missing or invalid respondent ID</li>
     *         <li>500 Internal Server Error - Unexpected error during processing</li>
     *         </ul>
     * @throws IllegalArgumentException if the request parameter is null
     * @see ReportRequest
     * @see FamilyHistoryReportResponse
     * @see FamilyHistoryReportService#generateAndUploadFamilyHistoryReport(Status)
     */
    @Path("/generate")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    @Transactional
    public Response generateFamilyHistoryReport(ReportRequest request) {
        try {
            LOG.info("Received family history report generation request for respondent: {}", request.id);
            
            // Validate request
            if (request.id == 0) {
                LOG.warn("Missing respondent ID in request");
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new FamilyHistoryReportResponse("Missing respondent ID", false))
                    .build();
            }

            Status status = Status.find("respondentId", request.id).firstResult();
            
            // Check if status record exists
            if (status == null) {
                LOG.warn("No status record found for respondent ID: {}", request.id);
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new FamilyHistoryReportResponse("No status record found for respondent", false))
                    .build();
            }
            
            LOG.info("Found status record for respondent {}: external ID = {}", request.id, status.getXid());

            // Start the asynchronous report generation and upload
            reportService.generateAndUploadFamilyHistoryReport(status);
            
            LOG.info("Family history report generation initiated for respondent: {}", request.id);
            
            return Response.ok(new FamilyHistoryReportResponse("Family history report generation initiated", true))
                    .build();
                    
        } catch (Exception e) {
            LOG.error("Failed to initiate family history report generation: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new FamilyHistoryReportResponse("Failed to generate report: " + e.getMessage(), false))
                    .build();
        }
    }
    
        /**
     * Health check endpoint to verify the service is running.
     * 
     * @return HTTP 200 OK response indicating the service is healthy
     */
    @Path("/health")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response healthCheck() {
        return Response.ok(new FamilyHistoryReportResponse("Family History Service is healthy", true))
                .build();
    }
    
    /**
     * Debug endpoint to check status records for a respondent.
     * 
     * @param respondentId the respondent ID to check
     * @return HTTP response with status information
     */
    @Path("/debug/status/{respondentId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response debugStatus(@PathParam("respondentId") long respondentId) {
        try {
            LOG.info("Checking status records for respondent: {}", respondentId);
            
            Status status = Status.find("respondentId", respondentId).firstResult();
            
            if (status == null) {
                LOG.warn("No status record found for respondent: {}", respondentId);
                return Response.ok(new FamilyHistoryReportResponse("No status record found for respondent " + respondentId, false))
                        .build();
            }
            
            LOG.info("Found status record for respondent {}: XID = {}", respondentId, status.getXid());
            return Response.ok(new FamilyHistoryReportResponse(
                    String.format("Status found - ID: %d, XID: %s, RespondentId: %d", 
                            status.getId(), status.getXid(), status.getRespondentId()), true))
                    .build();
                    
        } catch (Exception e) {
            LOG.error("Failed to check status for respondent {}: {}", respondentId, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new FamilyHistoryReportResponse("Error checking status: " + e.getMessage(), false))
                    .build();
        }
    }

    /**
     * Response object for family history report operations.
     * 
     * <p>This class encapsulates the response data returned by the family history
     * service endpoints. It provides a standardized way to communicate operation
     * results, including success status and descriptive messages.</p>
     * 
     * @author Elicit Software
     * @version 1.0
     * @since 1.0
     */
    public static class FamilyHistoryReportResponse {
        
        /**
         * Descriptive message about the operation result.
         * This field contains human-readable information about what occurred
         * during the operation, whether successful or not.
         */
        public String message;
        
        /**
         * Indicates whether the operation was successful.
         * {@code true} if the operation completed successfully,
         * {@code false} if an error occurred.
         */
        public boolean success;
        
        /**
         * Default no-argument constructor.
         * Creates an instance with default values (null message, false success).
         */
        public FamilyHistoryReportResponse() {}
        
        /**
         * Constructor to create a response with specified message and success status.
         * 
         * @param message descriptive message about the operation result
         * @param success {@code true} if the operation was successful, {@code false} otherwise
         */
        public FamilyHistoryReportResponse(String message, boolean success) {
            this.message = message;
            this.success = success;
        }
    }
}
