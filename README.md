# TraceRoot — AI-Powered Incident Detection & Root Cause Analysis Platform

## Overview

TraceRoot is a backend system that transforms raw application logs into structured incidents and generates AI-powered summaries to accelerate debugging and root cause analysis.

Instead of treating logs as passive data, TraceRoot models them as signals — detecting recurring failure patterns, grouping them into incidents, and providing actionable insights through a structured pipeline.

This project is designed to reflect how modern observability platforms operate, while remaining fully implementable and understandable at the application layer.

---

## Problem

Modern backend systems generate large volumes of logs, but:

* Logs are unstructured and difficult to interpret at scale
* Failures are often buried in noise
* Engineers manually correlate repeated errors across time
* There is no built-in mechanism to group, prioritize, or explain failures

As a result, incident detection and triage become slow, reactive, and error-prone.

---

## Solution

TraceRoot introduces a structured pipeline that converts logs into actionable incidents:

1. Logs are ingested and normalized
2. Repeated error patterns are detected using fingerprinting
3. Errors within a time window are grouped into incidents
4. Incidents are tracked through a lifecycle (ACTIVE → RESOLVED)
5. AI generates summaries, likely causes, and recommended checks

This shifts logs from raw output into structured, queryable, and explainable system behavior.

---

## Key Features

* Structured log ingestion with validation and normalization
* PostgreSQL-backed persistence layer
* Filtered log search (by service, level, metadata)
* Incident detection using fingerprinting:
  `(serviceName + level + exceptionType + endpoint)`
* Time-window-based detection (e.g., 3 matching errors within 5 minutes)
* Incident lifecycle management (ACTIVE / RESOLVED)
* Incident-to-log relationship via query-based mapping
* AI-powered incident summaries (stubbed LLM client with structured prompts)
* Modular monolith architecture with clear domain boundaries

---

## Architecture

The system is implemented as a modular monolith using Spring Boot.

```
ingestion/   → log intake, normalization, persistence  
search/      → log querying and filtering  
incident/    → detection, grouping, lifecycle management  
ai/          → prompt construction, LLM integration, summaries  
```

Each module has a clear responsibility and communicates through well-defined service boundaries.

---

## System Flow

1. Logs are submitted via API
2. Logs are normalized and persisted
3. ERROR logs trigger fingerprint evaluation
4. Matching logs within a time window are counted
5. If threshold is met, an incident is created
6. Incident aggregates matching logs
7. AI generates a structured summary based on logs and metadata

---

## AI Integration

TraceRoot includes a prompt-driven AI layer designed for backend reasoning.

* Structured prompt builder (incident + logs)
* Controlled context size (log sampling)
* Strict JSON output format
* Stubbed LLM client (replaceable with real provider)

Example output:

```
{
  "summary": "...",
  "possibleCause": "...",
  "recommendedChecks": [...]
}
```

The AI layer is intentionally decoupled from the core system, allowing easy integration with providers such as OpenAI or Anthropic.

---

## API Endpoints

### Logs

* `POST /api/logs`
* `GET /api/logs`
* `GET /api/logs/{id}`

### Incidents

* `GET /api/incidents`
* `GET /api/incidents/{id}`
* `POST /api/incidents/{id}/resolve`

### Incident Logs

* `GET /api/incidents/{id}/logs`

### AI Summary

* `GET /api/incidents/{id}/summary`

---

## Tech Stack

* Java 17
* Spring Boot
* PostgreSQL
* JPA / Hibernate
* REST APIs
* Prompt-based AI integration

---

## Running the Project

1. Clone the repository
2. Set up PostgreSQL locally
3. Configure `application.properties`
4. Run the Spring Boot application
5. Use Postman to interact with APIs

---

## Design Considerations

* Separation of ingestion vs query concerns
* Deterministic incident detection (before AI involvement)
* Consistent data normalization to avoid matching inconsistencies
* Time-window-based logic to reduce false positives
* AI used as an explanatory layer, not a decision engine

---

## Future Improvements

* Persist AI summaries and introduce caching
* Replace stub LLM with real API integration
* Add streaming ingestion (Kafka)
* Introduce anomaly detection beyond rule-based thresholds
* Build frontend dashboard for incident visualization
* Support multi-service distributed simulation

---

## Why This Project Matters

This project demonstrates:

* Backend system design beyond CRUD APIs
* Real-world observability patterns
* Data normalization and consistency strategies
* Incident detection and lifecycle modeling
* Clean service-layer architecture
* Practical AI integration in backend systems

It is intentionally designed to resemble systems used in platforms like Datadog, Sentry, and New Relic.

---

## Author

Built as a backend + AI systems project focused on observability, incident detection, and intelligent debugging workflows.
