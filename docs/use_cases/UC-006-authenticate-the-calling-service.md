# Use Case: Authenticate the Calling Service

## Overview

**Use Case ID:** UC-006
**Use Case Name:** Authenticate the Calling Service
**Primary Actor:** Survey Platform
**Secondary Actor:** Deployment Operator
**Goal:** Answer a request only when it carries the platform's service token, and present that token when FHHS itself calls another platform service, so that reaching the port is not enough to obtain a report.
**Status:** Implemented

## Preconditions

- The deployment supplies one service token to every module (`ELICIT_SERVICE_TOKEN`, read as `elicit.service.token`), at least 32 characters long (C-010).
- Where the survey definition's report addresses are not the origin of `pedigree.url`, the deployment lists their origins in `ELICIT_SERVICE_TRUSTED_ORIGINS` (`elicit.service.trusted-origins`), comma-separated, each `scheme://host[:port]`.

## Main Success Scenario

1. The Survey Platform sends a request to any FHHS endpoint with `Authorization: Bearer <service token>`.
2. The system compares the presented value with the configured token (BR-003) before it selects a resource (BR-001).
3. The values are equal; the request proceeds to the use case it addresses (UC-001 to UC-004), subject to UC-005.
4. When that use case calls the Pedigree Drawing Service, or a report at a trusted origin, the system presents the same token on its own call (BR-006).

## Alternative Flows

### A1: No Token, or Not a Bearer Credential

**Trigger:** The request has no `Authorization` header, or one that is not `Bearer <value>` (step 1).
**Flow:**

1. The system answers 401 with `WWW-Authenticate: Bearer` and an empty body. No resource runs and no respondent data is read. Use case ends.

### A2: Wrong Token

**Trigger:** The presented value differs from the configured token (step 2).
**Flow:**

1. The system answers exactly as in A1: the caller cannot tell a wrong token from a missing one. Use case ends.

### A3: No Token Is Configured

**Trigger:** `ELICIT_SERVICE_TOKEN` is unset or blank when FHHS starts.
**Flow:**

1. The system starts and writes one warning naming the variable.
2. The readiness probe reports not-ready through a check named `service-token` whose reason is "ELICIT_SERVICE_TOKEN is not set".
3. Every request, with or without a header, is answered 503 with the body `{"error":"service token not configured"}` (BR-002).
4. The system makes no outbound service call: a pedigree diagram is replaced by its placeholder (UC-003 A1) and a report that would be fetched for the delivery carries the error (UC-004 BR-004).
5. The operator sets the variable and restarts FHHS. Use case continues at step 1.

### A4: The Configured Token Is Too Short

**Trigger:** `ELICIT_SERVICE_TOKEN` is set to fewer than 32 characters.
**Flow:**

1. The start fails. The message names the variable and the length required, and carries neither the value nor its length. Use case ends.

### A5: A Trusted-Origin Entry Is Not an Origin

**Trigger:** An entry of `ELICIT_SERVICE_TRUSTED_ORIGINS` has no scheme or host, a scheme other than `http` or `https`, user information, a path, a query or a fragment.
**Flow:**

1. The start fails with a message naming the variable and the entry, so that a typing mistake is found by the operator and not by a call that silently went without the token. Use case ends.

### A6: The Development Token Is in Use

**Trigger:** The configured token is the well-known development value that the compose stack and the `%dev` profile supply.
**Flow:**

1. The system writes one warning at startup saying so, and otherwise behaves as in the main scenario.

### A7: A Report Address Is Not at a Trusted Origin

**Trigger:** A report address taken from the survey definition has an origin that is neither the origin of `pedigree.url` nor listed in `elicit.service.trusted-origins` (step 4).
**Flow:**

1. The system calls the address without the token and writes a warning naming the origin and the variable to list it in. What the called service answers is handled as for any report call (UC-004 BR-004).

## Postconditions

### Success Postconditions

- The request was served only because it carried the token; outbound calls carried it only to trusted origins.

### Failure Postconditions

- The request was answered 401 or 503 before any resource ran. Nothing was read from the database on its behalf.

## Business Rules

### BR-001: Every Endpoint, No List

The check runs before a resource is matched, so it covers every REST path FHHS serves, paths added later, and paths that do not exist (which answer 401, not 404, to a caller without the token). Nothing is exempted. The probes under `/q/health` stay open because Quarkus serves them outside the REST layer, not because they are listed.

### BR-002: Fail Closed

With no token configured FHHS refuses every request with 503. It never serves unauthenticated.

### BR-003: Constant-Time Comparison

The presented and the configured value are each digested with SHA-256 and the digests compared with `MessageDigest.isEqual`, so neither the content nor the length of the token can be learned from timing.

### BR-004: The Token Is Never Recorded

No log record, span attribute, exception message, health check or response body carries the configured token or a presented header. The access log pattern does not include the `Authorization` header, and REST client logging stays off.

### BR-005: Authentication Comes First

The token is checked before the survey check of UC-005. A caller without the token gets 401 and learns nothing else; only an authenticated caller is told that the survey is missing or unbuilt.

### BR-006: Where FHHS Sends the Token

FHHS presents the token to the Pedigree Drawing Service at `pedigree.url` on every call, and to a report address only when its origin is trusted. An origin is the lower-case scheme, the lower-case host and the effective port (80 or 443 when none is given). The address is parsed as a URI and its origin must equal a trusted origin exactly: there is no wildcard, prefix or suffix matching and no name resolution, and an address with user information, without a host, or with a scheme other than `http` or `https` never receives the token. The trusted origins are the entries of `elicit.service.trusted-origins` and the origin of `pedigree.url`. No redirect is followed on these calls.

### BR-007: A Minimum Length

A configured token has at least 32 characters (A4). `openssl rand -hex 32` produces a suitable one.

### BR-008: Authentication Is Not Authorization

The token says that a platform module is calling. It carries no identity and no role, and FHHS holds none: the calling module decided on whose behalf the report is requested before it called.

---

## Reference

Traces to FR-007, NFR-001 and C-010. Implemented by `ServiceToken` (the configured value, the
startup checks and warnings, the comparison and the trusted-origin rule), `ServiceTokenFilter`
(the 401 and the 503), `ServiceTokenHealthCheck` (readiness), `pedigree.Service.callPedigree` and
`PDFService.callReport` (the two outbound calls). `FamilyHistorySurveyRequiredFilter` runs after
it (BR-005). Verified by `ServiceTokenTest`, `ServiceTokenFilterTest`, `ServiceTokenEndpointsTest`,
`ServiceTokenUnsetTest`, `pedigree.ServiceTest`, `PDFServiceCallReportTest` and
`ResourceInventoryTest`.

### Status Values

| Status      | Description                                      |
|-------------|--------------------------------------------------|
| Draft       | Initial version, still being written.            |
| Reviewed    | Complete, awaiting stakeholder review.           |
| Approved    | Reviewed and approved for implementation.        |
| Implemented | Implementation complete, pending testing.        |
| Tested      | All tests pass, pending final acceptance.        |
| Done        | Fully implemented, tested, and accepted.         |
| Obsolete    | No longer valid, superseded by another use case. |
