package com.elicitsoftware.pedigree.error;

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

public class RelationshipError extends Exception {

	public RelationshipError() {
		super();
	}
	public RelationshipError(String message) {
		super(message);
	}
}
