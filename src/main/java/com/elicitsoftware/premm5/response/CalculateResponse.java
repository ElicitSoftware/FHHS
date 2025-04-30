package com.elicitsoftware.premm5.response;

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

import java.math.RoundingMode;
import java.text.DecimalFormat;
public class CalculateResponse {

    private String errorMessage;
    private double mlh1;
    private double msh2;
    private double msh6;
    private double pms2;
    private double any;
    private double none;

    public CalculateResponse() {
        super();
    }

    public CalculateResponse(String errorMessage, double mlh1, double msh2, double msh6, double pms2, double any,
                             double none) {
        super();
        this.errorMessage = errorMessage;
        this.mlh1 = mlh1;
        this.msh2 = msh2;
        this.msh6 = msh6;
        this.pms2 = pms2;
        this.any = any;
        this.none = none;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public double getMlh1() {
        return mlh1;
    }

    public void setMlh1(double mlh1) {

        this.mlh1 = mlh1;
    }

    public double getMsh2() {
        return msh2;
    }

    public void setMsh2(double msh2) {
        this.msh2 = msh2;
    }

    public double getMsh6() {
        return msh6;
    }

    public void setMsh6(double msh6) {
        this.msh6 = msh6;
    }

    public double getPms2() {
        return pms2;
    }

    public void setPms2(double pms2) {
        this.pms2 = pms2;
    }

    public double getAny() {
        return any;
    }

    public String getAnyRounded() {
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df.format(this.any);
    }

    public void setAny(double any) {
        this.any = any;
    }

    public double getNone() {
        return none;
    }

    public void setNone(double none) {
        this.none = none;
    }
}
