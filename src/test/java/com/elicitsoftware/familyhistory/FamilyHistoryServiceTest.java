package com.elicitsoftware.familyhistory;

/*-
 * ***LICENSE_START***
 * Elicit FHHS
 * %%
 * Copyright (C) 2025 - 2026 Elicit Software LLC
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import com.elicitsoftware.common.security.RespondentScope;
import com.elicitsoftware.model.Status;
import com.elicitsoftware.request.ReportRequest;
import com.elicitsoftware.test.PostgresTestResource;
import com.elicitsoftware.test.ServiceCalls;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Tests for {@code FamilyHistoryService}'s two endpoints - {@code /generate} and {@code /health}
 * - the REST entry point the Survey Platform calls per UC-004
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

    /** Accepts every respondent: these tests are about the delivery, RespondentScopeTest about the scope. */
    @InjectMock
    RespondentScope respondentScope;

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
        ServiceCalls.given()
                .contentType(ContentType.JSON)
                .body(requestFor(0))
                .when().post("/familyhistory/generate")
                .then()
                .statusCode(400);

        verifyNoInteractions(reportService);
    }

    /** UC-004 A4: no body at all. */
    @Test
    void generateFamilyHistoryReport_noBody_returnsBadRequestWithoutStartingGeneration() {
        ServiceCalls.given()
                .contentType(ContentType.JSON)
                .when().post("/familyhistory/generate")
                .then()
                .statusCode(400);

        verifyNoInteractions(reportService);
    }

    /** UC-004 A4: a body without a respondent ID, and one that is not positive. */
    @Test
    void generateFamilyHistoryReport_emptyOrNegativeRespondentId_returnsBadRequest() {
        for (String body : new String[] {"{}", "{\"id\":-1}"}) {
            ServiceCalls.given()
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when().post("/familyhistory/generate")
                    .then()
                    .statusCode(400);
        }

        verifyNoInteractions(reportService);
    }

    @Test
    void generateFamilyHistoryReport_noStatusRecordFound_returnsBadRequest() {
        stubStatusFind(42L, null);

        ServiceCalls.given()
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

        ServiceCalls.given()
                .contentType(ContentType.JSON)
                .body(requestFor(42L))
                .when().post("/familyhistory/generate")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("message", equalTo("Family history report generation initiated"));

        verify(reportService).generateAndUploadFamilyHistoryReport(argThat(s -> s.getRespondentId() == 42L));
    }

    /** UC-004 BR-009: the caller gets a fixed sentence; the cause stays in the log. */
    @Test
    void generateFamilyHistoryReport_reportServiceThrows_returnsInternalServerErrorWithAFixedMessage() {
        Status status = new Status();
        status.setRespondentId(42L);
        stubStatusFind(42L, status);
        when(reportService.generateAndUploadFamilyHistoryReport(any())).thenThrow(new RuntimeException("jdbc:postgresql://db:5432/survey cause-marker"));

        ServiceCalls.given()
                .contentType(ContentType.JSON)
                .body(requestFor(42L))
                .when().post("/familyhistory/generate")
                .then()
                .statusCode(500)
                .body("success", equalTo(false))
                .body("message", equalTo(FamilyHistoryService.START_FAILED_MESSAGE))
                .body(not(containsString("jdbc")))
                .body(not(containsString("cause-marker")));
    }

    // ------------------------------------------------------------------
    // NFR-008: no diagnostic endpoint
    // ------------------------------------------------------------------

    @Test
    void statusLookupPath_isNotServed() {
        ServiceCalls.given()
                .when().get("/familyhistory/debug/status/99")
                .then()
                .statusCode(404);
    }
}
