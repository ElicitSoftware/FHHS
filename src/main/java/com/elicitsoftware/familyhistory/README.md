# Family History Report Service

This service automatically generates family history PDF reports and uploads them to an SFTP server when family history surveys are completed.

## Overview

The Family History Report Service consists of several components:

1. **FamilyHistoryReportService** - Main service that orchestrates PDF generation and SFTP upload
2. **FamilyHistoryPdfGenerator** - Generates comprehensive PDF reports from family history data
3. **SftpService** - Handles file uploads to SFTP server (currently simulated using local filesystem)
4. **FamilyHistoryService** - REST endpoint for triggering report generation
5. **Database Migration** - Creates post_survey_actions table and configures automatic triggering

## Features

- **Automatic PDF Generation**: Creates comprehensive family history reports including:
  - Family member summary
  - Detailed member information (name, relationship, age, gender, cancer history)
  - Cancer history summary with counts by cancer type
- **Metadata XML Generation**: Creates XML metadata files with:
  - Respondent ID
  - External ID
  - Generation date
  - Report type
  - File listings
- **SFTP Upload**: Uploads both PDF and XML files to configured SFTP server
- **Asynchronous Processing**: Report generation runs in background to avoid blocking survey completion
- **Configurable**: All SFTP settings configurable via properties

## Configuration

Add the following properties to your `application.properties`:

```properties
# Family History SFTP Configuration
# Set to false to disable the SFTP upload feature entirely (skips the startup
# connection test, report generation/upload, and the retry job).
family.history.sftp.enabled=true
family.history.sftp.host=your-sftp-host.com
family.history.sftp.username=your_username
family.history.sftp.password=your_password
family.history.sftp.path=/uploads/family_history
family.history.sftp.port=22
family.history.sftp.timeout=30000

# Development Environment
%dev.family.history.sftp.host=localhost
%dev.family.history.sftp.username=dev_user
%dev.family.history.sftp.password=dev_password
%dev.family.history.sftp.path=/tmp/family_history_reports
%dev.family.history.sftp.port=2222
```

## Database Setup

The service includes a database migration that:

1. Creates the `survey.post_survey_actions` table
2. Adds post-survey actions for the FHHS survey to automatically trigger report generation

The migration file is located at:
`src/main/resources/db/migration/V0.0.4__CREATE_POST_SURVEY_ACTIONS.sql`

## API Endpoints

### Generate Family History Report

**POST** `/familyhistory/generate`

Triggers family history report generation for a respondent. Like every FHHS endpoint it
requires the platform service token (`Authorization: Bearer <ELICIT_SERVICE_TOKEN>`,
UC-006); a request without it is answered 401.

**Request Body:**
```json
{
  "id": 12345
}
```

**Response:**
```json
{
  "message": "Family history report generation initiated",
  "success": true
}
```

### Health

Liveness and readiness are the standard probes, `GET /q/health/live` and
`GET /q/health/ready`. They need no token. Readiness names what is missing: the
service token, the database, or the Family History Survey.

## File Output

The service generates two files per respondent:

1. **PDF Report** (`{external_id}.pdf`):
   - Comprehensive family history report
   - Includes all family members and their cancer history
   - Formatted as a professional medical report

2. **XML Metadata** (`{external_id}.xml`):
   ```xml
   <familyHistoryReport>
     <respondent id="12345"/>
     <externalId>STUDY_001_PARTICIPANT_001</externalId>
     <generatedDate>2025-01-18T10:30:00</generatedDate>
     <reportType>FAMILY_HISTORY</reportType>
     <files>
       <file type="PDF">STUDY_001_PARTICIPANT_001.pdf</file>
       <file type="METADATA">STUDY_001_PARTICIPANT_001.xml</file>
     </files>
   </familyHistoryReport>
   ```

## Integration with Survey System

The service integrates with the survey system through post-survey actions:

1. When a family history survey is completed (respondent.finalized_dt is set)
2. The Survey system looks up post_survey_actions for that survey
3. It makes HTTP POST calls to the configured URLs
4. Our family history service receives the call and generates/uploads the report

## Testing

### Manual Testing

You can test the service manually by calling the endpoint:

```bash
curl -X POST http://localhost:8082/familyhistory/generate \
  -H "Authorization: Bearer $ELICIT_SERVICE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id": 1}'
```

### Health

```bash
curl http://localhost:8082/q/health/ready
```

## Development Notes

### Current SFTP Implementation

The current SFTP service is implemented as a simulation that writes files to the local filesystem. To implement actual SFTP:

1. Add JSch dependency to `pom.xml`:
   ```xml
   <dependency>
     <groupId>com.jcraft</groupId>
     <artifactId>jsch</artifactId>
     <version>0.1.55</version>
   </dependency>
   ```

2. Replace the `SftpService` implementation with the JSch-based version (commented code available)

### PDF Generation

The current PDF generator creates a simple text-based representation. For production use, consider:

1. Using Apache PDFBox for proper PDF formatting
2. Adding company logos and branding
3. Implementing more sophisticated table layouts
4. Adding charts and visualizations

### Error Handling

The service includes comprehensive error handling and logging. Monitor the logs for:

- Failed report generations
- SFTP connection issues
- Data validation problems

## Security Considerations

1. **SFTP Credentials**: Store SFTP credentials securely (consider using Quarkus Vault extension)
2. **Access Control**: Every endpoint requires the platform service token (UC-006). Supply `ELICIT_SERVICE_TOKEN` (at least 32 characters, the same value for every module) from the deployment's secret store, and keep FHHS on a private network as well
3. **Data Encryption**: Consider encrypting sensitive data in transit and at rest
4. **Audit Logging**: Implement audit trails for report generation activities

## Future Enhancements

1. **Real SFTP Implementation**: Replace simulation with actual SFTP client
2. **Advanced PDF Features**: Add charts, graphs, and better formatting
3. **Email Notifications**: Send notifications when reports are generated
4. **Report Templates**: Support multiple report templates
5. **Batch Processing**: Process multiple reports efficiently
6. **Report History**: Track and store report generation history
