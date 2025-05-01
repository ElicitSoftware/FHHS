package com.elicitsoftware.premm5.request;

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

public class CalculateRequest {

    public boolean isMale;
    public int numberOfProbandsColorectalCancers;
    public boolean patientHasEndometrialCancer;
    public boolean patientHasOtherLynchSyndromeCancer;
    public int numberOfFirstDRsWithColorectalCancer;
    public int numberOfSecondDRsWithColorectalCancer;
    public int numberOfFirstDRsWithEndometrialCancer;
    public int numberOfSecondDRsWithEndometrialCancer;
    public int numberOfFirstDRsWithLynchSyndromeCancer;
    public int numberOfSecondDRsWithLynchSyndromeCancer;
    public int minAgePatientColorectalCancer;
    public int minAgeFirstDRsColorectalCancer;
    public int minAgeSecondDRsColorectalCancer;
    public int minAgePatientEndometrialCancer;
    public int minAgeFirstDRsEndometrialCancer;
    public int minAgeSecondDRsEndometrialCancer;
    public int probandAge;

    public CalculateRequest() {
        super();
    }
}
