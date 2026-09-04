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
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@code FamilyHistoryReportService#generateXmlMetadata(Status)}, the placeholder
 * substitution that builds the SFTP-uploaded XML metadata sidecar file. This is private and
 * driven entirely by {@code @ConfigProperty}-injected fields with no CDI/DB dependency, so it
 * is invoked here via reflection rather than through {@code @QuarkusTest}.
 */
class FamilyHistoryReportServiceXmlTest {

    private FamilyHistoryReportService newService(String xmlTemplate) throws Exception {
        FamilyHistoryReportService service = new FamilyHistoryReportService();
        setField(service, "xmlTemplate", xmlTemplate);
        return service;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = FamilyHistoryReportService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private String invokeGenerateXmlMetadata(FamilyHistoryReportService service, Status status) throws Exception {
        Method method = FamilyHistoryReportService.class.getDeclaredMethod("generateXmlMetadata", Status.class);
        method.setAccessible(true);
        return (String) method.invoke(service, status);
    }

    private Status statusWith(long respondentId, String xid, String firstName) {
        Status status = new Status();
        status.setRespondentId(respondentId);
        status.setXid(xid);
        status.setFirstName(firstName);
        status.setSurveyId(9L);
        status.setDepartmentId(3L);
        return status;
    }

    @Test
    void generateXmlMetadata_substitutesRespondentIdAndXid() throws Exception {
        FamilyHistoryReportService service = newService("<pdf><externalSourceId>{RespondentId}</externalSourceId><mrn>{Xid}</mrn></pdf>");
        Status status = statusWith(42L, "XID-99", "Jane");

        String xml = invokeGenerateXmlMetadata(service, status);

        assertEquals("<pdf><externalSourceId>42</externalSourceId><mrn>XID-99</mrn></pdf>", xml);
    }

    @Test
    void generateXmlMetadata_nullOptionalFields_substituteAsEmptyStringNotTheLiteralNull() throws Exception {
        FamilyHistoryReportService service = newService("<n>{FirstName}</n><l>{LastName}</l><e>{Email}</e>");
        Status status = statusWith(1L, "X", null); // firstName left null
        status.setLastName(null);
        status.setEmail(null);

        String xml = invokeGenerateXmlMetadata(service, status);

        assertEquals("<n></n><l></l><e></e>", xml);
        assertFalse(xml.contains("null"), "a null field must never render the literal string 'null': " + xml);
    }

    @Test
    void generateXmlMetadata_dobNull_substitutesEmptyString() throws Exception {
        FamilyHistoryReportService service = newService("<dob>{Dob}</dob>");
        Status status = statusWith(1L, "X", "Jane");
        status.setDob(null);

        String xml = invokeGenerateXmlMetadata(service, status);

        assertEquals("<dob></dob>", xml);
    }

    @Test
    void generateXmlMetadata_surveyIdAndDepartmentId_substituteAsPlainIntegers() throws Exception {
        FamilyHistoryReportService service = newService("<s>{SurveyId}</s><d>{DepartmentID}</d>");
        Status status = statusWith(1L, "X", "Jane");

        String xml = invokeGenerateXmlMetadata(service, status);

        assertEquals("<s>9</s><d>3</d>", xml);
    }

    @Test
    void generateXmlMetadata_createdAndFinalized_useStatusDateFormatting() throws Exception {
        FamilyHistoryReportService service = newService("<c>{Created}</c><f>{Finalized}</f>");
        Status status = statusWith(1L, "X", "Jane");

        String xml = invokeGenerateXmlMetadata(service, status);

        assertEquals("<c>" + status.getCreated() + "</c><f>" + status.getFinalized() + "</f>", xml);
    }

    @Test
    void generateXmlMetadata_unrecognizedPlaceholder_isLeftUnsubstituted() throws Exception {
        FamilyHistoryReportService service = newService("<x>{NotAPlaceholder}</x>");
        Status status = statusWith(1L, "X", "Jane");

        String xml = invokeGenerateXmlMetadata(service, status);

        assertEquals("<x>{NotAPlaceholder}</x>", xml);
    }
}
