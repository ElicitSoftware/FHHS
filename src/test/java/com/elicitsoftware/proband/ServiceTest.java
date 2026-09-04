package com.elicitsoftware.proband;

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

import com.elicitsoftware.model.CancerHistoryRepository;
import com.elicitsoftware.model.FamilyHistoryRecord;
import com.elicitsoftware.request.ReportRequest;
import com.elicitsoftware.response.ReportResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@code proband.Service#report(ReportRequest)}, invoked directly as a plain
 * method (bypassing JAX-RS/CDI, which need a live Postgres to boot in this environment).
 */
class ServiceTest {

    private static FamilyHistoryRecord record(String step, String breastCancer, Integer age, String multiple) {
        return new FamilyHistoryRecord(
                step, null, null, null, null,
                null, null, null,
                null, null,
                breastCancer, age, null,
                null, null,
                null, null,
                null, null,
                null, null,
                null, null,
                null, null,
                null, null,
                null, null,
                null, null,
                null, null,
                null, null, null,
                null, null,
                null, null,
                null, null,
                null, null,
                null, null,
                null, null,
                null, null,
                null, null,
                null, multiple,
                null, null,
                null, null,
                null, null,
                null, null,
                null, null,
                null, null
        );
    }

    private Service newService(CancerHistoryRepository repo) {
        Service service = new Service();
        service.cancerHistoryRepository = repo;
        return service;
    }

    @Test
    void report_onlyProbandAndProbandCancerStepsAreIncluded() {
        CancerHistoryRepository repo = Mockito.mock(CancerHistoryRepository.class);
        Mockito.when(repo.findFamilyHistoryByRespondentId(1L)).thenReturn(List.of(
                record("Proband", "true", 40, "false"),
                record("Proband Cancer", "true", 41, "false"),
                record("Sibling", "true", 20, "false")
        ));

        ReportRequest req = new ReportRequest();
        req.id = 1L;
        ReportResponse response = newService(repo).report(req);

        // Two proband-step records each contribute 3 rows (age, triple-negative, multiple -
        // see RowConverterTest); the sibling record must be excluded entirely from this report.
        assertEquals(6, response.pdf.content[0].table.body.length);
    }

    @Test
    void report_noProbandRecord_rendersNoSignificantDataMessage() {
        CancerHistoryRepository repo = Mockito.mock(CancerHistoryRepository.class);
        Mockito.when(repo.findFamilyHistoryByRespondentId(1L)).thenReturn(List.of(
                record("Sibling", "true", 20, "false")
        ));

        ReportRequest req = new ReportRequest();
        req.id = 1L;
        ReportResponse response = newService(repo).report(req);

        assertEquals("No Significant Data.", response.innerHTML);
        assertEquals(0, response.pdf.content[0].table.body.length);
    }

    @Test
    void report_emptyRepositoryResult_rendersNoSignificantDataMessage() {
        CancerHistoryRepository repo = Mockito.mock(CancerHistoryRepository.class);
        Mockito.when(repo.findFamilyHistoryByRespondentId(1L)).thenReturn(List.of());

        ReportRequest req = new ReportRequest();
        req.id = 1L;
        ReportResponse response = newService(repo).report(req);

        assertEquals("No Significant Data.", response.innerHTML);
    }

    @Test
    void report_alwaysReturnsRespondentSummaryTitle() {
        CancerHistoryRepository repo = Mockito.mock(CancerHistoryRepository.class);
        Mockito.when(repo.findFamilyHistoryByRespondentId(1L)).thenReturn(List.of());

        ReportRequest req = new ReportRequest();
        req.id = 1L;
        ReportResponse response = newService(repo).report(req);

        assertEquals("Respondent Summary", response.title);
        assertEquals("Respondent Summary", response.pdf.title);
    }

    @Test
    void report_probandTableRowUsesLabelAndValueColumnsOnly() {
        CancerHistoryRepository repo = Mockito.mock(CancerHistoryRepository.class);
        Mockito.when(repo.findFamilyHistoryByRespondentId(1L)).thenReturn(List.of(
                record("Proband", "true", 50, "true")
        ));

        ReportRequest req = new ReportRequest();
        req.id = 1L;
        ReportResponse response = newService(repo).report(req);

        String[][] body = response.pdf.content[0].table.body;
        assertEquals("Breast Cancer", body[0][0]);
        assertEquals("50", body[0][1]);
    }

    @Test
    void report_passesRequestedRespondentIdToRepository() {
        CancerHistoryRepository repo = Mockito.mock(CancerHistoryRepository.class);
        Mockito.when(repo.findFamilyHistoryByRespondentId(Mockito.anyLong())).thenReturn(List.of());

        ReportRequest req = new ReportRequest();
        req.id = 555L;
        newService(repo).report(req);

        Mockito.verify(repo).findFamilyHistoryByRespondentId(555L);
    }
}
