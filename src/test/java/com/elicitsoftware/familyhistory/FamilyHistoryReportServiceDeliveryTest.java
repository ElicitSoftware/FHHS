package com.elicitsoftware.familyhistory;

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

import com.elicitsoftware.model.Status;
import com.elicitsoftware.test.LogCapture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * UC-004 step 3-5, BR-007 and BR-008: what one delivery names its two files, when it refuses to
 * start, and what it writes to the log. {@code doGenerateAndUploadFamilyHistoryReport} is called
 * directly on a plain instance with a mocked {@link SftpService}; PDF generation, which needs a
 * CDI request context, is replaced by a fixed byte array.
 */
class FamilyHistoryReportServiceDeliveryTest {

    private static final byte[] PDF = "%PDF-test".getBytes(StandardCharsets.UTF_8);
    private static final String TEMPLATE =
            "<pdf><externalSourceId>{RespondentId}</externalSourceId><mrn>{Xid}</mrn></pdf>";

    private final SftpService sftp = mock(SftpService.class);
    private int pdfGenerations;

    private FamilyHistoryReportService newService() throws Exception {
        FamilyHistoryReportService service = new FamilyHistoryReportService() {
            @Override
            public byte[] generateFamilyHistoryPdf(Long respondentId) {
                pdfGenerations++;
                return PDF;
            }
        };
        service.sftpService = sftp;
        Field template = FamilyHistoryReportService.class.getDeclaredField("xmlTemplate");
        template.setAccessible(true);
        template.set(service, TEMPLATE);
        return service;
    }

    private static Status status(long respondentId, String xid) {
        Status status = new Status();
        status.setRespondentId(respondentId);
        status.setXid(xid);
        status.setFirstName("Zzyzx");
        status.setAccessCode("AccessMarker1");
        status.setSurveyId(9L);
        status.setDepartmentId(3L);
        return status;
    }

    // ------------------------------------------------------------------
    // BR-007: file names
    // ------------------------------------------------------------------

    /**
     * NFR-012: for an ordinary XID the two names are what they always were. Inside the metadata the
     * XID is XML-escaped (UC-004 BR-005), so it is the same bytes unless it has a markup character.
     */
    @ParameterizedTest
    @ValueSource(strings = {"12345678", "A-1_b.2", "MRN 0042", "XID-99", ".hidden", "-rf", "O'Brien&Co"})
    void ordinaryXid_namesBothFilesExactlyAfterIt(String xid) throws Exception {
        newService().doGenerateAndUploadFamilyHistoryReport(status(42L, xid));

        ArgumentCaptor<byte[]> xml = ArgumentCaptor.forClass(byte[].class);
        InOrder order = inOrder(sftp);
        order.verify(sftp).uploadFile(eq(xid + ".pdf"), eq(PDF));
        order.verify(sftp).uploadFile(eq(xid + "-index.xml"), xml.capture());
        verifyNoMoreInteractions(sftp);
        assertEquals("<pdf><externalSourceId>42</externalSourceId><mrn>" + FamilyHistoryReportService.xmlEscape(xid) + "</mrn></pdf>",
                new String(xml.getValue(), StandardCharsets.UTF_8));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", ".", "..", "../x", "a/b", "a\\b", "/abs", "x\ny", "x\u0000y"})
    void xidThatCannotNameAFile_failsBeforeAnythingIsGeneratedOrUploaded(String xid) throws Exception {
        FamilyHistoryReportService service = newService();

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> service.doGenerateAndUploadFamilyHistoryReport(status(42L, xid)));

        assertEquals(FamilyHistoryReportService.INVALID_XID_MESSAGE, failure.getMessage());
        assertEquals(0, pdfGenerations, "nothing is generated");
        verifyNoInteractions(sftp);
    }

    @Test
    void refusalMessage_doesNotRepeatTheXid() {
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> FamilyHistoryReportService.deliveryBaseName("../XID-MARKER-99"));

        assertFalse(failure.getMessage().contains("XID-MARKER-99"));
    }

    @Test
    void deliveryBaseName_returnsAUsableXidUnchanged() {
        assertEquals("12345678", FamilyHistoryReportService.deliveryBaseName("12345678"));
        assertEquals(" padded ", FamilyHistoryReportService.deliveryBaseName(" padded "));
    }

    // ------------------------------------------------------------------
    // BR-008, NFR-009: the log
    // ------------------------------------------------------------------

    @Test
    void successfulDelivery_isLoggedByRespondentIdWithoutXidNameOrAccessCode() throws Exception {
        FamilyHistoryReportService service = newService();

        try (LogCapture log = LogCapture.start()) {
            service.doGenerateAndUploadFamilyHistoryReport(status(4242L, "XID-MARKER-99"));

            assertNoRespondentData(log);
            assertTrue(log.contains("4242") || log.contains("4,242"), log.text());
            assertTrue(log.contains("bytes"), log.text());
        }
    }

    @Test
    void failedUpload_isLoggedByRespondentIdWithoutXidNameOrAccessCode() throws Exception {
        FamilyHistoryReportService service = newService();
        doThrow(new RuntimeException("Failed to upload file to SFTP server")).when(sftp).uploadFile(anyString(), any());

        try (LogCapture log = LogCapture.start()) {
            assertThrows(RuntimeException.class,
                    () -> service.doGenerateAndUploadFamilyHistoryReport(status(4242L, "XID-MARKER-99")));

            assertNoRespondentData(log);
            assertTrue(log.contains("Failed to upload the PDF file"), log.text());
        }
    }

    @Test
    void refusedXid_isNotLogged() throws Exception {
        FamilyHistoryReportService service = newService();

        try (LogCapture log = LogCapture.start()) {
            assertThrows(IllegalStateException.class,
                    () -> service.doGenerateAndUploadFamilyHistoryReport(status(4242L, "../XID-MARKER-99")));

            assertNoRespondentData(log);
        }
    }

    private static void assertNoRespondentData(LogCapture log) {
        assertFalse(log.contains("XID-MARKER-99"), log.text());
        assertFalse(log.contains("Zzyzx"), log.text());
        assertFalse(log.contains("AccessMarker1"), log.text());
        assertFalse(log.contains("<pdf>"), log.text());
    }

    /** UC-004 BR-005: an XID with markup characters names the files as it is and is escaped inside the metadata. */
    @Test
    void xidWithMarkupCharacters_isEscapedInTheMetadataOnly() throws Exception {
        newService().doGenerateAndUploadFamilyHistoryReport(status(42L, "O'Brien&Co"));

        ArgumentCaptor<byte[]> xml = ArgumentCaptor.forClass(byte[].class);
        verify(sftp).uploadFile(eq("O'Brien&Co.pdf"), eq(PDF));
        verify(sftp).uploadFile(eq("O'Brien&Co-index.xml"), xml.capture());
        assertEquals("<pdf><externalSourceId>42</externalSourceId><mrn>O&apos;Brien&amp;Co</mrn></pdf>",
                new String(xml.getValue(), StandardCharsets.UTF_8));
    }
}
