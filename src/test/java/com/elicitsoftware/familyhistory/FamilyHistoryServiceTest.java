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
import com.elicitsoftware.test.PostgresTestResource;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Tests for {@code FamilyHistoryService}'s three endpoints - {@code /generate}, {@code /health},
 * and {@code /debug/status/{id}} - the REST entry point the Survey Platform calls per UC-004
 * step 1. Run as a full {@code @QuarkusTest} (over HTTP via RestAssured), because
 * {@code Status.find(...)} is a Panache active-record static finder: it only exists once
 * Quarkus's build-time entity enhancement has run, and {@code survey.status} is itself a
 * read-only database view (owned by the Survey app), so it can't be seeded with a direct
 * insert either. {@code PanacheMock} is the Quarkus-supported way to mock such static finders,
 * and only works inside a booted Quarkus test context - hence this needs the same reachable
 * Postgres as {@code FamilyHistoryReportServiceTest} in this package.
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class FamilyHistoryServiceTest {

    @InjectMock
    FamilyHistoryReportService reportService;

    @BeforeEach
    void setUp() {
        PanacheMock.mock(Status.class);
        reset(reportService);
    }

    @SuppressWarnings("unchecked")
    private void stubStatusFind(long respondentId, Status result) {
        PanacheQuery<Status> query = mock(PanacheQuery.class);
        when(query.firstResult()).thenReturn(result);
        when(Status.<Status>find("respondentId", respondentId)).thenReturn(query);
    }

    private ReportRequest requestFor(long respondentId) {
        ReportRequest request = new ReportRequest();
        request.id = respondentId;
        return request;
    }

    // ------------------------------------------------------------------
    // generateFamilyHistoryReport
    // ------------------------------------------------------------------

    @Test
    void generateFamilyHistoryReport_missingRespondentId_returnsBadRequestWithoutStartingGeneration() {
        given()
                .contentType(ContentType.JSON)
                .body(requestFor(0))
                .when().post("/familyhistory/generate")
                .then()
                .statusCode(400)
                .body("success", equalTo(false))
                .body("message", equalTo("Missing respondent ID"));

        verifyNoInteractions(reportService);
    }

    @Test
    void generateFamilyHistoryReport_noStatusRecordFound_returnsBadRequest() {
        stubStatusFind(42L, null);

        given()
                .contentType(ContentType.JSON)
                .body(requestFor(42L))
                .when().post("/familyhistory/generate")
                .then()
                .statusCode(400)
                .body("success", equalTo(false))
                .body("message", equalTo("No status record found for respondent"));

        verifyNoInteractions(reportService);
    }

    @Test
    void generateFamilyHistoryReport_statusFound_startsGenerationAndReturnsOk() {
        Status status = new Status();
        status.setRespondentId(42L);
        status.setXid("EXT-1");
        stubStatusFind(42L, status);

        given()
                .contentType(ContentType.JSON)
                .body(requestFor(42L))
                .when().post("/familyhistory/generate")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("message", equalTo("Family history report generation initiated"));

        verify(reportService).generateAndUploadFamilyHistoryReport(argThat(s -> s.getRespondentId() == 42L));
    }

    @Test
    void generateFamilyHistoryReport_reportServiceThrows_returnsInternalServerError() {
        Status status = new Status();
        status.setRespondentId(42L);
        stubStatusFind(42L, status);
        when(reportService.generateAndUploadFamilyHistoryReport(any())).thenThrow(new RuntimeException("boom"));

        given()
                .contentType(ContentType.JSON)
                .body(requestFor(42L))
                .when().post("/familyhistory/generate")
                .then()
                .statusCode(500)
                .body("success", equalTo(false))
                .body("message", equalTo("Failed to generate report: boom"));
    }

    // ------------------------------------------------------------------
    // healthCheck
    // ------------------------------------------------------------------

    @Test
    void healthCheck_alwaysReturnsOk() {
        given()
                .when().get("/familyhistory/health")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("message", equalTo("Family History Service is healthy"));
    }

    // ------------------------------------------------------------------
    // debugStatus
    // ------------------------------------------------------------------

    @Test
    void debugStatus_noStatusRecordFound_returnsOkWithFailureFlag() {
        stubStatusFind(99L, null);

        given()
                .when().get("/familyhistory/debug/status/99")
                .then()
                .statusCode(200)
                .body("success", equalTo(false))
                .body("message", equalTo("No status record found for respondent 99"));
    }

    @Test
    void debugStatus_statusFound_returnsOkWithDetails() {
        Status status = new Status();
        status.setId(7L);
        status.setRespondentId(99L);
        status.setXid("EXT-99");
        stubStatusFind(99L, status);

        given()
                .when().get("/familyhistory/debug/status/99")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("message", equalTo("Status found - ID: 7, XID: EXT-99, RespondentId: 99"));
    }

    @Test
    void debugStatus_lookupThrows_returnsInternalServerError() {
        when(Status.<Status>find("respondentId", 99L)).thenThrow(new RuntimeException("db down"));

        given()
                .when().get("/familyhistory/debug/status/99")
                .then()
                .statusCode(500)
                .body("success", equalTo(false))
                .body("message", equalTo("Error checking status: db down"));
    }
}
