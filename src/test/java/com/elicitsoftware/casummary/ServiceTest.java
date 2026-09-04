package com.elicitsoftware.casummary;

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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@code casummary.Service#report(ReportRequest)}, invoked directly as a plain
 * method (bypassing JAX-RS/CDI, which need a live Postgres to boot in this environment).
 * The {@link CancerHistoryRepository} dependency is mocked so these tests characterize the
 * report-assembly logic on top of whatever rows the (Kimball-affected) repository returns.
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
    void report_excludesProbandRecordsFromOutput() {
        CancerHistoryRepository repo = Mockito.mock(CancerHistoryRepository.class);
        List<FamilyHistoryRecord> facts = new ArrayList<>();
        facts.add(record("Proband", "true", 40, "false"));
        facts.add(record("Sibling", "true", 30, "false"));
        Mockito.when(repo.findFamilyHistoryByRespondentId(1L)).thenReturn(facts);

        ReportRequest req = new ReportRequest();
        req.id = 1L;
        ReportResponse response = newService(repo).report(req);

        assertFalse(response.innerHTML.contains("No relatives reported cancer."));
        assertTrue(response.innerHTML.contains("Sibling"));
    }

    @Test
    void report_noRelativeCancer_rendersNoRelativesMessageInHtmlAndPdf() {
        CancerHistoryRepository repo = Mockito.mock(CancerHistoryRepository.class);
        Mockito.when(repo.findFamilyHistoryByRespondentId(1L)).thenReturn(new ArrayList<>());

        ReportRequest req = new ReportRequest();
        req.id = 1L;
        ReportResponse response = newService(repo).report(req);

        assertEquals("No relatives reported cancer.", response.innerHTML);
        assertEquals(2, response.pdf.content.length);
        assertEquals("No relatives reported cancer.", response.pdf.content[1].text);
    }

    @Test
    void report_onlyProbandHasCancer_stillRendersNoRelativesMessage() {
        CancerHistoryRepository repo = Mockito.mock(CancerHistoryRepository.class);
        Mockito.when(repo.findFamilyHistoryByRespondentId(1L))
                .thenReturn(new ArrayList<>(List.of(record("Proband", "true", 40, "false"))));

        ReportRequest req = new ReportRequest();
        req.id = 1L;
        ReportResponse response = newService(repo).report(req);

        assertEquals("No relatives reported cancer.", response.innerHTML);
    }

    @Test
    void report_relativeWithCancer_buildsPdfTableWithOneRowPerFinding() {
        CancerHistoryRepository repo = Mockito.mock(CancerHistoryRepository.class);
        Mockito.when(repo.findFamilyHistoryByRespondentId(1L))
                .thenReturn(new ArrayList<>(List.of(record("Mother", "true", 55, "true"))));

        ReportRequest req = new ReportRequest();
        req.id = 1L;
        ReportResponse response = newService(repo).report(req);

        // Breast Cancer unconditionally contributes 3 rows: age, triple-negative (even though
        // unset here), and the multiple-diagnosis flag. See RowConverterTest.
        String[][] body = response.pdf.content[0].table.body;
        assertEquals(3, body.length);
        assertEquals("Mother", body[0][0]);
        assertEquals("Breast Cancer", body[0][1]);
        assertEquals("55", body[0][2]);
    }

    @Test
    void report_alwaysReturnsCancerSummaryTitle() {
        CancerHistoryRepository repo = Mockito.mock(CancerHistoryRepository.class);
        Mockito.when(repo.findFamilyHistoryByRespondentId(1L)).thenReturn(new ArrayList<>());

        ReportRequest req = new ReportRequest();
        req.id = 1L;
        ReportResponse response = newService(repo).report(req);

        assertEquals("Cancer Summary", response.title);
        assertEquals("Cancer Summary", response.pdf.title);
    }

    @Test
    void report_passesRequestedRespondentIdToRepository() {
        CancerHistoryRepository repo = Mockito.mock(CancerHistoryRepository.class);
        Mockito.when(repo.findFamilyHistoryByRespondentId(Mockito.anyLong())).thenReturn(new ArrayList<>());

        ReportRequest req = new ReportRequest();
        req.id = 987L;
        newService(repo).report(req);

        Mockito.verify(repo).findFamilyHistoryByRespondentId(987L);
    }
}
