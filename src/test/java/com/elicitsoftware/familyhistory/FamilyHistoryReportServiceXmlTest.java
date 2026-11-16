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
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

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

    /** UC-004 BR-005: {AccessCode} carries the respondent's survey access code. */
    @Test
    void generateXmlMetadata_accessCode_substitutesRespondentAccessCode() throws Exception {
        FamilyHistoryReportService service = newService("<code>{AccessCode}</code>");
        Status status = statusWith(1L, "X", "Jane");
        status.setAccessCode("Bx7kQ2mNp");

        String xml = invokeGenerateXmlMetadata(service, status);

        assertEquals("<code>Bx7kQ2mNp</code>", xml);
    }

    /** UC-004 BR-005: a missing access code substitutes as empty text. */
    @Test
    void generateXmlMetadata_accessCodeNull_substitutesEmptyString() throws Exception {
        FamilyHistoryReportService service = newService("<code>{AccessCode}</code>");
        Status status = statusWith(1L, "X", "Jane");

        String xml = invokeGenerateXmlMetadata(service, status);

        assertEquals("<code></code>", xml);
    }

    /** UC-004 BR-005: the former {Token} placeholder was renamed without an alias. */
    @Test
    void generateXmlMetadata_formerTokenPlaceholder_isLeftUnsubstituted() throws Exception {
        FamilyHistoryReportService service = newService("<code>{Token}</code>");
        Status status = statusWith(1L, "X", "Jane");
        status.setAccessCode("Bx7kQ2mNp");

        String xml = invokeGenerateXmlMetadata(service, status);

        assertEquals("<code>{Token}</code>", xml);
    }

    @Test
    void generateXmlMetadata_unrecognizedPlaceholder_isLeftUnsubstituted() throws Exception {
        FamilyHistoryReportService service = newService("<x>{NotAPlaceholder}</x>");
        Status status = statusWith(1L, "X", "Jane");

        String xml = invokeGenerateXmlMetadata(service, status);

        assertEquals("<x>{NotAPlaceholder}</x>", xml);
    }

    // ------------------------------------------------------------------
    // UC-004 BR-005: the template is filled in a single pass
    // ------------------------------------------------------------------

    /** A value that reads like a placeholder is placed as text and never filled in turn. */
    @Test
    void generateXmlMetadata_placeholderTextInsideAValue_staysLiteral() throws Exception {
        FamilyHistoryReportService service = newService("<mrn>{Xid}</mrn><n>{FirstName}</n><code>{AccessCode}</code>");
        Status status = statusWith(1L, "{AccessCode}", "{LastName}");
        status.setLastName("Doe");
        status.setAccessCode("Bx7kQ2mNp");

        String xml = invokeGenerateXmlMetadata(service, status);

        assertEquals("<mrn>{AccessCode}</mrn><n>{LastName}</n><code>Bx7kQ2mNp</code>", xml);
    }

    @Test
    void generateXmlMetadata_accessCodeAppearsOnlyWhereTheTemplateAsksForIt() throws Exception {
        FamilyHistoryReportService service = newService("<mrn>{Xid}</mrn><n>{FirstName}</n>");
        Status status = statusWith(1L, "{AccessCode}", "{AccessCode}");
        status.setAccessCode("Bx7kQ2mNp");

        String xml = invokeGenerateXmlMetadata(service, status);

        assertFalse(xml.contains("Bx7kQ2mNp"), xml);
    }

    /** Characters the replacement engine treats specially arrive unchanged. */
    @Test
    void generateXmlMetadata_dollarAndBackslashInAValue_arePlacedUnchanged() throws Exception {
        FamilyHistoryReportService service = newService("<n>{FirstName}</n><l>{LastName}</l>");
        Status status = statusWith(1L, "X", "$1 and ${Xid}");
        status.setLastName("back\\slash");

        String xml = invokeGenerateXmlMetadata(service, status);

        assertEquals("<n>$1 and ${Xid}</n><l>back\\slash</l>", xml);
    }

    /**
     * NFR-012: for ordinary values the file is byte for byte what substituting the placeholders
     * one after another produces - the reference below is that sequential substitution.
     */
    @Test
    void generateXmlMetadata_ordinaryValues_matchSequentialSubstitutionByteForByte() throws Exception {
        String template = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <pdf>
                    <externalSourceId>{RespondentId}</externalSourceId>
                    <mrn>{Xid}</mrn>
                    <name first="{FirstName}" middle="{MiddleName}">{LastName}, {FirstName}</name>
                    <contact>{Email} {Phone}</contact>
                    <department id="{DepartmentID}">{DepartmentName}</department>
                    <survey>{SurveyId}</survey><code>{AccessCode}</code><status>{Status}</status>
                    <dob>{Dob}</dob><created>{Created}</created><finalized>{Finalized}</finalized>
                    <kept>{Token} {NotAPlaceholder} {} { Xid }</kept>
                </pdf>
                """;
        Status status = statusWith(42L, "12345678", "Mary-Jane");
        status.setLastName("O'Brien");
        status.setMiddleName("Q.");
        status.setEmail("mj@example.org");
        status.setPhone("(734) 555-0100");
        status.setDepartmentName("Genetics & Oncology");
        status.setAccessCode("Bx7kQ2mNp");
        status.setStatus("Finalized");

        String sequential = template
                .replace("{Xid}", "12345678")
                .replace("{RespondentId}", "42")
                .replace("{SurveyId}", "9")
                .replace("{FirstName}", "Mary-Jane")
                .replace("{LastName}", "O'Brien")
                .replace("{MiddleName}", "Q.")
                .replace("{Dob}", status.getDob() != null ? status.getDob().toString() : "")
                .replace("{Email}", "mj@example.org")
                .replace("{Phone}", "(734) 555-0100")
                .replace("{DepartmentName}", "Genetics & Oncology")
                .replace("{DepartmentID}", "3")
                .replace("{AccessCode}", "Bx7kQ2mNp")
                .replace("{Status}", "Finalized")
                .replace("{Created}", status.getCreated())
                .replace("{Finalized}", status.getFinalized());

        String xml = invokeGenerateXmlMetadata(newService(template), status);

        assertArrayEquals(sequential.getBytes(StandardCharsets.UTF_8), xml.getBytes(StandardCharsets.UTF_8));
    }

    /** The template a deployment gets when it configures none starts with the XML declaration. */
    @Test
    void defaultTemplate_startsWithTheXmlDeclaration() throws Exception {
        String defaultTemplate = FamilyHistoryReportService.class.getDeclaredField("xmlTemplate")
                .getAnnotation(ConfigProperty.class).defaultValue();

        assertTrue(defaultTemplate.startsWith("<?xml version=\"1.0\""), defaultTemplate);

        String xml = invokeGenerateXmlMetadata(newService(defaultTemplate), statusWith(42L, "12345678", "Jane"));
        assertTrue(xml.contains("<externalSourceId>42</externalSourceId>"), xml);
        assertTrue(xml.contains("<mrn>12345678</mrn>"), xml);
    }
}
