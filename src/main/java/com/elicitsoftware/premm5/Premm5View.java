package com.elicitsoftware.premm5;

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

import com.elicitsoftware.premm5.request.CalculateRequest;
import jakarta.persistence.Transient;
public class Premm5View {

    private static final String TRUE = "true";
    private static final String MALE = "male";

    public long respondentId;
    public String gender;
    public String CRC;
    public boolean CRCMultiple;
    public Integer crcMinAge;
    public String ec;
    public String ls;
    public Integer fdrCrc;
    public Integer sdrCrc;
    public Integer fdrEc;
    public Integer sdrEc;
    public Integer fdrLS;
    public Integer sdrLs;
    public Integer fdrMinCrc;
    public Integer sdrMinCrc;
    public Integer ecMinAge;
    public Integer fdrMinEc;
    public Integer sdrMinEc;
    public Integer age;
    public CalculateRequest getCalculateRequest() {
        CalculateRequest req = new CalculateRequest();

        req.isMale = MALE.equalsIgnoreCase(this.gender);

        if (TRUE.equalsIgnoreCase(this.CRC)) {
            if(this.CRCMultiple == true) {
                req.numberOfProbandsColorectalCancers = 2;
            }else {
                req.numberOfProbandsColorectalCancers = 1;
            }
        }

        if (TRUE.equalsIgnoreCase(this.ec)) {
            req.patientHasEndometrialCancer = true;
        }

        if (TRUE.equalsIgnoreCase(this.ls)) {
            req.patientHasOtherLynchSyndromeCancer = true;
        }

        if (this.fdrCrc != null) {
            req.numberOfFirstDRsWithColorectalCancer = this.fdrCrc;
        }
        if (this.sdrCrc != null) {
            req.numberOfSecondDRsWithColorectalCancer = this.sdrCrc;
        }

        if (this.fdrEc != null) {
            req.numberOfFirstDRsWithEndometrialCancer = this.fdrEc;
        }

        if (this.sdrEc != null) {
            req.numberOfSecondDRsWithEndometrialCancer = this.sdrEc;
        }

        if (this.fdrLS != null) {
            req.numberOfFirstDRsWithLynchSyndromeCancer = this.fdrLS;
        }

        if (this.sdrLs != null) {
            req.numberOfSecondDRsWithLynchSyndromeCancer = this.sdrLs;
        }

        if (this.crcMinAge != null) {
            req.minAgePatientColorectalCancer = this.crcMinAge;
        }

        if (this.fdrMinCrc != null) {
            req.minAgeFirstDRsColorectalCancer = this.fdrMinCrc;
        }

        if (this.sdrMinCrc != null) {
            req.minAgeSecondDRsColorectalCancer = this.sdrMinCrc;
        }

        if (this.ecMinAge != null) {
            req.minAgePatientEndometrialCancer = this.ecMinAge;
        }

        if (this.fdrMinEc != null) {
            req.minAgeFirstDRsEndometrialCancer = this.fdrMinEc;
        }

        if (this.sdrMinEc != null) {
            req.minAgeSecondDRsEndometrialCancer = this.sdrMinEc;
        }

        if (this.age != null) {
            req.probandAge = this.age;
        }

        return req;
    }
    @Transient
    public boolean familyHasCancerDX() {
        boolean crc = false;
        boolean ec = false;
        boolean ls = false;
        boolean fcrc = false;
        boolean scrc = false;
        boolean fec = false;
        boolean sec = false;
        boolean fls = false;
        boolean sls = false;

        if (TRUE.equalsIgnoreCase(this.CRC)) {
            crc = true;
        }

        if (TRUE.equalsIgnoreCase(this.ec)) {
            ec = true;
        }

        if (TRUE.equalsIgnoreCase(this.ls)) {
            ls = true;
        }

        if (this.fdrCrc != null && this.fdrCrc > 0) {
            fcrc = true;
        }

        if (this.sdrCrc != null && this.sdrCrc > 0) {
            scrc = true;
        }

        if (this.fdrEc != null && this.fdrEc > 0) {
            fec = true;
        }

        if (this.sdrEc != null && this.sdrEc > 0) {
            sec = true;
        }

        if (this.fdrLS != null && this.fdrLS > 0) {
            fls = true;
        }

        if (this.sdrLs != null && this.sdrLs > 0) {
            sls = true;
        }

        return crc || ec || ls || fcrc || scrc || fec || sec || fls || sls;

    }
}
