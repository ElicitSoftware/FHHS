# Elicit FHHS 3.0.0-rc.1

**Release date:** to be set when tagged

FHHS is released with the Elicit platform, at the platform's version. The platform's release
notes (`docs/release-notes/3.0.0-rc.1.md` in the umbrella repository, ElicitSoftware/Elicit)
cover the upgrade from 2.x and the changes FHHS shares with Survey and Admin; what concerns FHHS
in particular:

## Breaking changes

- **The Family History Survey arrives by import.** FHHS no longer installs it. Apply
  `family-history-survey.elicit` in Admin > Apply Survey Definition. Until then FHHS starts,
  reports not ready on `/q/health/ready` and answers report requests with 503; it becomes ready
  on its next health check after the import, without a restart (UC-005).
- **The service token is required.** Every path outside `/q` needs `ELICIT_SERVICE_TOKEN` as a
  bearer token; without a configured token FHHS reports not ready and answers 503.
- **SFTP delivery is off by default** (`family.history.sftp.enabled=true` turns it on), and the
  server's host key is verified against `family.history.sftp.knownHosts` (UC-004 BR-006).
- **The SFTP XML template's `{Token}` is `{AccessCode}`** (UC-004 BR-005).
- **No default database passwords.**
- **Removed:** `GET /familyhistory/health` (use `/q/health/ready`) and the endpoints under
  `/familyhistory/debug`.

## Changes

- FHHS reads the survey's own reporting schema, whatever it is named.
- Report output is encoded and report requests are validated; a failing report service's answer
  stays out of the report and the log (UC-004 BR-004).
- SVG rendering refuses external resources and scripts; Apache Batik 1.20.
- SFTP moves to `com.github.mwiede:jsch`.
- Survey definition fixes: every placeholder is delimited, `{S1}` on the Mother and Father steps
  is filled from an answer, and defaultless placeholders get an empty default.

# Elicit FHHS 0.8.0 (2025-04-30)

## Initial Release
Initial Release of Elicit Software's Family Health History Survey (FHHS)
