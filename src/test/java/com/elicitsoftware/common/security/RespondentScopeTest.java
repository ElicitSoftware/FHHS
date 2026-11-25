package com.elicitsoftware.common.security;

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

import com.elicitsoftware.familyhistory.FamilyHistoryReportService;
import com.elicitsoftware.model.CancerHistoryRepository;
import com.elicitsoftware.model.FamilyManager;
import com.elicitsoftware.test.LogCapture;
import com.elicitsoftware.test.PostgresTestResource;
import com.elicitsoftware.test.ServiceCalls;
import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * UC-006 A8 and BR-009 (NFR-014), and the matching flows of the report use cases (UC-001 A4,
 * UC-002 A4, UC-003 A3, UC-004 A5): a report or a delivery is produced only for a respondent of
 * the Family History Survey. A respondent of another survey and an ID that does not exist are
 * answered 404 alike, before any report data is read; a respondent of the survey is served.
 *
 * <p>The respondents are real rows, written through the owner connection because the second
 * survey is one: the scope is a query and is tested as one.</p>
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class RespondentScopeTest {

    /** A respondent of the Family History Survey (survey 1 of the fixture). */
    private static final long IN_SURVEY = 880001;
    /** A respondent of another survey. */
    private static final long OTHER_SURVEY = 880002;
    /** No respondent has this ID. */
    private static final long UNKNOWN = 880003;

    @Inject
    RespondentScope scope;

    @Inject
    @DataSource("owner")
    AgroalDataSource ownerDataSource;

    @InjectMock
    CancerHistoryRepository cancerHistoryRepository;

    @InjectMock
    FamilyManager familyManager;

    @InjectMock
    FamilyHistoryReportService reportService;

    @BeforeEach
    void seed() throws SQLException {
        execute("INSERT INTO survey.surveys(id, survey_key, display_order, name, title) VALUES "
                        + "(8801, '0b0f6c0e-6f0a-4f8e-9d55-1d8f7c1a8801', 88, 'Another Survey', 'Another')",
                "INSERT INTO survey.respondents(id, survey_id, access_code) VALUES "
                        + "(" + IN_SURVEY + ", 1, 'SCOPE-IN'), (" + OTHER_SURVEY + ", 8801, 'SCOPE-OTHER')");
        clearInvocations(cancerHistoryRepository, familyManager, reportService);
    }

    @AfterEach
    void remove() throws SQLException {
        execute("DELETE FROM survey.respondents WHERE id IN (" + IN_SURVEY + ", " + OTHER_SURVEY + ")",
                "DELETE FROM survey.surveys WHERE id = 8801");
    }

    private void execute(String... statements) throws SQLException {
        try (Connection connection = ownerDataSource.getConnection();
             Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        }
    }

    @Test
    @Transactional
    void containsOnlyRespondentsOfTheFamilyHistorySurvey() {
        assertTrue(scope.contains(IN_SURVEY));
        assertFalse(scope.contains(OTHER_SURVEY), "a respondent of another survey");
        assertFalse(scope.contains(UNKNOWN), "an ID no respondent has");
        assertFalse(scope.contains(0));
        assertFalse(scope.contains(-1));
        assertFalse(scope.contains(Integer.MAX_VALUE + 1L), "an ID no respondent can have");
    }

    /** UC-001 A4, UC-002 A4, UC-003 A3, UC-004 A5: another survey's respondent is not found, and nothing is read. */
    @ParameterizedTest
    @ValueSource(strings = {"/proband/report", "/casummary/report", "/pedigree/report", "/familyhistory/generate"})
    void aRespondentOfAnotherSurvey_isAnswered404(String path) {
        refused(path, OTHER_SURVEY);
    }

    /** The same answer for an ID that does not exist, so the answer does not say which it was. */
    @ParameterizedTest
    @ValueSource(strings = {"/proband/report", "/casummary/report", "/pedigree/report", "/familyhistory/generate"})
    void anUnknownRespondent_isAnswered404(String path) {
        refused(path, UNKNOWN);
    }

    private void refused(String path, long respondentId) {
        ServiceCalls.given().contentType(ContentType.JSON).body("{\"id\":" + respondentId + "}")
                .when().post(path)
                .then().statusCode(404)
                .body(equalTo(RespondentScope.NOT_FOUND_MESSAGE));

        verifyNoInteractions(cancerHistoryRepository, familyManager, reportService);
    }

    /** UC-001 A1 still holds: a respondent of the survey with nothing recorded gets the empty report. */
    @ParameterizedTest
    @ValueSource(strings = {"/proband/report", "/casummary/report"})
    void aRespondentOfTheSurvey_isServed(String path) {
        ServiceCalls.given().contentType(ContentType.JSON).body("{\"id\":" + IN_SURVEY + "}")
                .when().post(path)
                .then().statusCode(200);

        verify(cancerHistoryRepository).findFamilyHistoryByRespondentId(IN_SURVEY);
    }

    /** UC-006 BR-009, NFR-009: an accepted request is recorded by endpoint and respondent ID, and by nothing else. */
    @Test
    void anAcceptedRequestIsRecordedByRespondentId() {
        try (LogCapture log = LogCapture.start()) {
            ServiceCalls.given().contentType(ContentType.JSON).body("{\"id\":" + IN_SURVEY + "}")
                    .when().post("/proband/report")
                    .then().statusCode(200);

            assertTrue(log.contains("Report request /proband/report for respondent " + IN_SURVEY), log.text());
            assertFalse(log.contains("SCOPE-IN"), "the access code is never logged");
        }
    }
}
