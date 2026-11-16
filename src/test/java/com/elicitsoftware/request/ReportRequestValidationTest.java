package com.elicitsoftware.request;

/*-
 * ***LICENSE_START***
 * Elicit FHHS
 * %%
 * Copyright (C) 2025 - 2026 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import com.elicitsoftware.model.CancerHistoryRepository;
import com.elicitsoftware.test.PostgresTestResource;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.restassured.RestAssured.given;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * UC-001 A3, UC-002 A3, UC-003 A2 (NFR-010): a report request with no body, or with a respondent
 * ID that is missing or not positive, is answered 400 and no respondent data is read.
 * {@code /familyhistory/generate} (UC-004 A4) is covered in {@code FamilyHistoryServiceTest}.
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class ReportRequestValidationTest {

    @InjectMock
    CancerHistoryRepository cancerHistoryRepository;

    @ParameterizedTest
    @ValueSource(strings = {"/proband/report", "/casummary/report", "/pedigree/report"})
    void noBody_isAnsweredBadRequest(String path) {
        given().contentType(ContentType.JSON)
                .when().post(path)
                .then().statusCode(400);

        verifyNoInteractions(cancerHistoryRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/proband/report", "/casummary/report", "/pedigree/report"})
    void missingRespondentId_isAnsweredBadRequest(String path) {
        given().contentType(ContentType.JSON).body("{}")
                .when().post(path)
                .then().statusCode(400);

        verifyNoInteractions(cancerHistoryRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/proband/report", "/casummary/report", "/pedigree/report"})
    void negativeRespondentId_isAnsweredBadRequest(String path) {
        given().contentType(ContentType.JSON).body("{\"id\":-1}")
                .when().post(path)
                .then().statusCode(400);

        verifyNoInteractions(cancerHistoryRepository);
    }
}
