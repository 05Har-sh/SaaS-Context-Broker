# SaaS Context Broker

A Spring Boot service that brokers incident context between Slack and Jira, evaluates incident severity, tracks timeline events, and exposes incident analytics APIs for dashboards and operations workflows.

## What it does

- Receives Slack and Jira incident updates through webhook-style endpoints
- Maintains an incident lifecycle (`CREATED`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`)
- Calculates severity and risk score based on urgency, Jira status, and staleness
- Sends Slack alerts for high/critical incidents with cooldown protection
- Tracks incident timeline events across system, Slack, and Jira sources
- Exposes metrics, health, trend, and prioritization endpoints

## Tech stack

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- PostgreSQL (runtime driver)
- Maven

## Project structure

`src/main/java/com/harsh/context_broker/contextBroker`

- `controller` - REST APIs
- `service` - core incident logic, alerting, timeline handling
- `entity` - JPA entities (`IncidentEntity`, `TimelineEventEntity`, `WebhookConfig`)
- `repository` - data access layer
- `dto` - request/response contracts
- `exception` - global error handling
- `specification` - filtering support for incident listing

## Configuration

Set these properties in your environment or Spring configuration:

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`
- `spring.jpa.hibernate.ddl-auto`
- `scoring.urgent`
- `scoring.jiraOpen`
- `scoring.jiraInProgress`
- `scoring.stale`
- `staleness.threshold.minutes`

## Run locally

```bash
./mvnw spring-boot:run
```

Or build and run:

```bash
./mvnw clean package
java -jar target/contextBroker-0.0.1-SNAPSHOT.jar
```

## Main API endpoints

### Incoming updates

- `POST /incoming/slack`
  - Body: `{ "incidentKey": "INC-101", "message": "URGENT: checkout failing" }`
- `POST /incoming/jira`
  - Body: `{ "incidentKey": "INC-101", "status": "IN_PROGRESS" }`

### Webhook configuration

- `POST /config/slack`
  - Body: `{ "webhookUrl": "https://hooks.slack.com/services/..." }`
- `POST /config/jira`
  - Body: `{ "webhookUrl": "https://your-jira-webhook-url" }`

### Incident APIs

- `GET /incident/{incidentKey}`
- `GET /incident/{incidentKey}/timeline`
- `GET /incident/incident-details/{incidentKey}`
- `GET /incident/metrics`
- `GET /incident/all?page=0&size=10&severity=HIGH&assignedTo=alice&stale=true&jiraStatus=OPEN`
- `GET /incident/system-health`
- `GET /incident/risk-score/{incidentKey}`
- `GET /incident/priority/{incidentKey}`
- `GET /incident/highest-risk`
- `GET /incident/severity-distribution`
- `GET /incident/trend`
- `POST /incident/{incidentKey}/assign`
  - Body: `{ "assignedTo": "alice" }`

## Enum values

- `JiraStatus`: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`
- `Severity`: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`
- `IncidentStatus`: `DETECTED`, `CREATED`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`

## Error handling

Global exception handling returns structured error payloads for:

- `400` validation failures
- `404` missing resources
- `500` unexpected server errors

