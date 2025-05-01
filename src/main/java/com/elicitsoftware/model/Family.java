package com.elicitsoftware.model;

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

import java.util.ArrayList;
import java.util.List;

public class Family {
    public List<FamilyMember> family = new ArrayList<>();

    public boolean hasMultipleCancers() {
        for (FamilyMember p : family) {
            if (p.hasMultipleCancers()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        // Add the headers
        StringBuilder sb = new StringBuilder("Ped	ID	Sex	Dadid	Momid	Status	Label	ul	ur	ll	lr");
        sb.append(System.lineSeparator());
        for (FamilyMember p : family) {
            sb.append(p.toString());
        }
        return sb.toString();
    }
}
