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
import com.elicitsoftware.premm5.response.CalculateResponse;
public class Premm5 {
    private String errorMessage;
    private double lpMLH1;
    private double lpMSH2;
    private double lpMSH6;
    private double lpPMS2;
    private final int v0;
    private final int v1;
    private final int v2;
    private final int v3;
    private final int v4;
    private final Double v5;
    private final Double v6;
    private final Double v7;
    private final int v8;
    private final int v9;
    private final int v10;
    public Premm5(CalculateRequest req) {
        // V0 Sex of patient, V0 = a if male 0 if female
        if (req.isMale) {
            this.v0 = 1;
        } else {
            this.v0 = 0;
        }

        // V1 = Patient has presence of one CRC
        // V2 = Patient has presence of two or more CRCs
        if (req.numberOfProbandsColorectalCancers == 0) {
            this.v1 = 0;
            this.v2 = 0;
        } else if (req.numberOfProbandsColorectalCancers < 2) {
            this.v1 = 1;
            this.v2 = 0;
        } else {
            this.v1 = 0;
            this.v2 = 1;
        }

        // V3 Patient has presence of Endometrial Cancer
        if (req.patientHasEndometrialCancer) {
            this.v3 = 1;
        } else {
            this.v3 = 0;
        }

        // V4 Patient has other Lynch Syndrome related cancers
        if (req.patientHasOtherLynchSyndromeCancer) {
            this.v4 = 1;
        } else {
            this.v4 = 0;
        }

        this.v5 = calculateV5(req.numberOfFirstDRsWithColorectalCancer, req.numberOfSecondDRsWithColorectalCancer);

        this.v6 = calculateV6(req.numberOfFirstDRsWithEndometrialCancer, req.numberOfSecondDRsWithEndometrialCancer);

        this.v7 = calculateV7(req.numberOfFirstDRsWithLynchSyndromeCancer,
                req.numberOfSecondDRsWithLynchSyndromeCancer);

        this.v8 = calculateV8(req.minAgePatientColorectalCancer, req.minAgeFirstDRsColorectalCancer,
                req.minAgeSecondDRsColorectalCancer);

        this.v9 = calculateV9(req.minAgePatientEndometrialCancer, req.minAgeFirstDRsEndometrialCancer,
                req.minAgeSecondDRsEndometrialCancer);

        this.v10 = calculateV10(req.probandAge);

    }
    private double calculateV5(int numberOfFirstDRsWithColorectalCancer, int numberOfSecondDRsWithColorectalCancer) {
        // V5 Presence of CRC in FDR and/or SDR =
        double a = 0;
        double b = 0;
        double c = 0;
        double d = 0;

        if (numberOfFirstDRsWithColorectalCancer == 1) {
            a = 1;
            b = 0;
        }

        if (numberOfFirstDRsWithColorectalCancer > 1) {
            a = 0;
            b = 2;
        }

        if (numberOfSecondDRsWithColorectalCancer == 1) {
            c = 0.5;
            d = 0;
        }

        if (numberOfSecondDRsWithColorectalCancer > 1) {
            c = 0;
            d = 1;
        }

        return a + b + c + d;
    }

    /**
     * Calculates the value for V6 based on the number of first-degree relatives
     * and second-degree relatives with endometrial cancer.
     *
     * @param numberOfFirstDRsWithEndometrialCancer  the number of first-degree relatives with endometrial cancer
     * @param numberOfSecondDRsWithEndometrialCancer the number of second-degree relatives with endometrial cancer
     * @return the calculated V6 value based on the provided inputs
     */
    private double calculateV6(int numberOfFirstDRsWithEndometrialCancer, int numberOfSecondDRsWithEndometrialCancer) {
        // V7 Presence of other LS related cancers in FDR and/or SDR
        double a = 0;
        double b = 0;
        double c = 0;
        double d = 0;

        if (numberOfFirstDRsWithEndometrialCancer == 1) {
            a = 1;
            b = 0;
        }
        if (numberOfFirstDRsWithEndometrialCancer > 1) {
            a = 0;
            b = 2;
        }

        if (numberOfSecondDRsWithEndometrialCancer == 1) {
            c = 0.5;
            d = 0;
        }
        if (numberOfSecondDRsWithEndometrialCancer > 1) {
            c = 0;
            d = 1;
        }

        return a + b + c + d;
    }

    /**
     * Calculates V7, which evaluates the presence of cancer associated with Lynch syndrome
     * in first-degree relatives (FDRs) and second-degree relatives (SDRs).
     *
     * @param numberOfFirstDRsWithLynchSyndromeCancer  the number of first-degree relatives with Lynch syndrome cancer
     * @param numberOfSecondDRsWithLynchSyndromeCancer the number of second-degree relatives with Lynch syndrome cancer
     * @return the calculated score based on the presence of Lynch syndrome cancer in FDRs and SDRs
     */
    private double calculateV7(int numberOfFirstDRsWithLynchSyndromeCancer,
                               int numberOfSecondDRsWithLynchSyndromeCancer) {
        // V6 Presence of EC in FDR and/or SDR
        double e = 0;
        double f = 0;

        if (numberOfFirstDRsWithLynchSyndromeCancer > 0) {
            e = 1;
        }
        if (numberOfSecondDRsWithLynchSyndromeCancer > 0) {
            f = 0.5;
        }

        return e + f;
    }

    /**
     * Calculates a value based on minimum ages related to colorectal cancer for the patient,
     * first-degree relatives, and second-degree relatives.
     * Adjustments are made to ensure the values fall within specified bounds.
     *
     * @param minAgePatientColorectalCancer   the minimum age of the patient with colorectal cancer
     * @param minAgeFirstDRsColorectalCancer  the minimum age of first-degree relatives with colorectal cancer
     * @param minAgeSecondDRsColorectalCancer the minimum age of second-degree relatives with colorectal cancer
     * @return the calculated value based on the adjusted minimum ages
     */
    private int calculateV8(int minAgePatientColorectalCancer, int minAgeFirstDRsColorectalCancer,
                            int minAgeSecondDRsColorectalCancer) {

        int a = minAgePatientColorectalCancer;
        int b = minAgeFirstDRsColorectalCancer;
        int c = minAgeSecondDRsColorectalCancer;

        if (minAgePatientColorectalCancer < 27) {
            a = 27;
        }

        if (minAgePatientColorectalCancer > 77) {
            a = 77;
        }

        if (minAgePatientColorectalCancer == 0) {
            a = 45;
        }

        if (minAgeFirstDRsColorectalCancer < 28) {
            b = 28;
        }

        if (minAgeFirstDRsColorectalCancer > 81) {
            b = 81;
        }

        if (minAgeFirstDRsColorectalCancer == 0) {
            b = 45;
        }

        if (minAgeSecondDRsColorectalCancer < 30) {
            c = 30;
        }

        if (minAgeSecondDRsColorectalCancer > 85) {
            c = 85;
        }

        if (minAgeSecondDRsColorectalCancer == 0) {
            c = 45;
        }

        return (a - 45) + (b - 45) + (c - 45);
    }

    /**
     * Calculates a value based on the minimum ages of endometrial cancer occurrence
     * in the patient and their first- and second-degree relatives. The method adjusts
     * the input values to predefined bounds if they fall outside specific ranges
     * and computes a resulting integer value.
     *
     * @param minAgePatientEndometrialCancer   the minimum age of the patient's endometrial cancer diagnosis
     * @param minAgeFirstDRsEndometrialCancer  the minimum age of endometrial cancer diagnosis in first-degree relatives
     * @param minAgeSecondDRsEndometrialCancer the minimum age of endometrial cancer diagnosis in second-degree relatives
     * @return an integer value derived from the processed minimum ages
     */
    private int calculateV9(int minAgePatientEndometrialCancer, int minAgeFirstDRsEndometrialCancer,
                            int minAgeSecondDRsEndometrialCancer) {
        int a = minAgePatientEndometrialCancer;
        int b = minAgeFirstDRsEndometrialCancer;
        int c = minAgeSecondDRsEndometrialCancer;

        if (minAgePatientEndometrialCancer < 31) {
            a = 31;
        }

        if (minAgePatientEndometrialCancer > 67) {
            a = 67;
        }

        if (minAgePatientEndometrialCancer == 0) {
            a = 45;
        }

        if (minAgeFirstDRsEndometrialCancer < 28) {
            b = 28;
        }

        if (minAgeFirstDRsEndometrialCancer > 69) {
            b = 69;
        }

        if (minAgeFirstDRsEndometrialCancer == 0) {
            b = 45;
        }

        if (minAgeSecondDRsEndometrialCancer < 34) {
            c = 34;
        }

        if (minAgeSecondDRsEndometrialCancer > 70) {
            c = 70;
        }

        if (minAgeSecondDRsEndometrialCancer == 0) {
            c = 45;
        }

        return (a - 45) + (b - 45) + (c - 45);
    }

    /**
     * Calculates the adjusted age value based on the provided proband age.
     * The method returns 22 if the proband age is less than 22, 83 if the proband age is greater than 83,
     * and the proband age itself if it falls within the range of 22 to 83 (inclusive).
     *
     * @param probandAge the age of the individual for whom the calculation is performed
     * @return the adjusted age value, constrained to the range of 22 to 83
     */
    private int calculateV10(int probandAge) {
        if (probandAge < 22) {
            return 22;
        } else if (probandAge > 83) {
            return 83;
        } else {
            return probandAge;
        }
    }

    /**
     * Calculates and assigns probabilities for specific genetic factors and their cumulative percentages.
     * The method utilizes logistic regression calculations for lpMLH1, lpMSH2, lpMSH6, and PMS2 to determine
     * the respective probabilities, then compiles them into a response object.
     *
     * @return a CalculateResponse object containing the probabilities for MLH1, MSH2, MSH6, PMS2,
     * the cumulative percentage for any of the factors, and the complementary percentage for none.
     */
    public CalculateResponse calculate() {

        this.lpMLH1 = calculate_lpMLH1();
        this.lpMSH2 = calculate_lpMSH2();
        this.lpMSH6 = calculate_lpMSH6();
        this.lpPMS2 = calculate_PMS2();

        CalculateResponse response = new CalculateResponse();

        double denominator = 1 + Math.exp(this.lpMLH1) + Math.exp(this.lpMSH2) + Math.exp(this.lpMSH6)
                + Math.exp(this.lpPMS2);
        response.setMlh1(Math.exp(this.lpMLH1) / denominator);

        response.setMsh2(Math.exp(this.lpMSH2) / denominator);

        response.setMsh6(Math.exp(this.lpMSH6) / denominator);

        response.setPms2(Math.exp(this.lpPMS2) / denominator);

        response.setAny((response.getMlh1() + response.getMsh2() + response.getMsh6() + response.getPms2()) * 100);

        response.setNone(100 - response.getAny());

        return response;

    }

    /**
     * Calculates the lpMLH1 (log probability for MLH1 gene mutation) value
     * based on the values of several input variables (v0 to v10).
     * <p>
     * The calculation uses a linear combination of coefficients applied
     * to the input variables, along with a constant term.
     *
     * @return the calculated lpMLH1 value as a double.
     */
    private double calculate_lpMLH1() {
        return -5.325 + (0.904 * this.v0) + (2.586 * this.v1) + (3.183 * this.v2) + (1.621 * this.v3)
                + (1.276 * this.v4) + (1.560 * this.v5) + (0.804 * this.v6) + (0.397 * this.v7) + (-0.0557 * this.v8)
                + (0.0115 * this.v9) + (-0.0476 * this.v10);
    }

    /**
     * Calculates the lpMSH2 value based on a predefined formula that utilizes
     * several independent variables (v0, v1, v2, ..., v10) as coefficients.
     *
     * @return the calculated lpMSH2 value as a double
     */
    private double calculate_lpMSH2() {

        return -4.427 + (0.937 * this.v0) + (1.799 * this.v1) + (2.593 * this.v2) + (1.924 * this.v3)
                + (1.585 * this.v4) + (1.337 * this.v5) + (0.670 * this.v6) + (0.607 * this.v7) + (-0.0441 * this.v8)
                + (0.0002 * this.v9) + (-0.0482 * this.v10);
    }

    /**
     * Calculates the lpMSH6 score based on various input variables.
     *
     * @return A double value representing the calculated lpMSH6 score.
     */
    private double calculate_lpMSH6() {
        return -4.675 + (0.816 * this.v0) + (1.265 * this.v1) + (-53.205 * this.v2) + (1.759 * this.v3)
                + (0.538 * this.v4) + (0.545 * this.v5) + (0.923 * this.v6) + (0.313 * this.v7) + (-0.0095 * this.v8)
                + (0.0344 * this.v9) + (-0.0363 * this.v10);
    }

    /**
     * Calculates the PMS2 mutation probability using a specific linear regression model
     * with predefined coefficients and values from the class fields.
     *
     * @return the calculated PMS2 mutation probability as a double value.
     */
    private double calculate_PMS2() {

        double returnV = -4.913 + (0.294 * this.v0) + (0.989 * this.v1) + (-0.354 * this.v2) + (0.739 * this.v3)
                + (0.395 * this.v4) + (-0.002 * this.v5) + (-0.426 * this.v6) + (-0.105 * this.v7) + (-0.0086 * this.v8)
                + (0.0008 * this.v9) + (-0.0074 * this.v10);
        return returnV;
    }

    /**
     * Retrieves the error message associated with the current instance.
     *
     * @return the error message as a String
     */
    public String getErrorMessage() {
        return errorMessage;
    }
}
