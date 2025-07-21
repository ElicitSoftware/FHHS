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
 * This service provides endpoints that can be called by post-survey actions
 * to generate and upload family history reports after a survey is completed.
 */
@Path("/familyhistory")
@RequestScoped
public class FamilyHistoryService {

    private static final Logger LOG = LoggerFactory.getLogger(FamilyHistoryService.class);
    
    @Inject
    FamilyHistoryReportService reportService;

    /**
     * Endpoint for generating and uploading a family history report.
     * This endpoint is designed to be called by post-survey actions
     * after a family history survey is completed.
     * 
     * @param request the report generation request
     * @return HTTP response indicating success or failure
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

            // Use respondent ID as external ID if not provided
            String externalId = status.getXid() != null ? status.getXid() : String.valueOf(request.id);
            
            // Start the asynchronous report generation and upload
            reportService.generateAndUploadFamilyHistoryReport(request.id, externalId);
            
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
     * Health check endpoint for the family history service.
     * 
     * @return HTTP 200 if service is healthy
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
     * Response object for family history report operations.
     */
    public static class FamilyHistoryReportResponse {
        public String message;
        public boolean success;
        
        public FamilyHistoryReportResponse() {}
        
        public FamilyHistoryReportResponse(String message, boolean success) {
            this.message = message;
            this.success = success;
        }
    }
}
