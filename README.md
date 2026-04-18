# TraceRoot — AI-Powered Reliability Platform

TraceRoot is a backend reliability platform that ingests application logs, detects recurring production failures, groups them into lifecycle-managed incidents, and generates AI-powered summaries to accelerate root cause analysis.

It consists of two systems:

1. **TraceRoot Platform** — the core reliability platform (log ingestion, incident detection, lifecycle management, AI summarization, metrics).
2. **Failure Lab** — a distributed microservices sandbox that generates realistic failure patterns (cascading failures, retry storms, timeouts, null pointers) to stress-test the platform with real distributed traffic.

The project is intentionally structured to resemble systems like Datadog, Sentry, and New Relic at the architectural level, while remaining fully implementable and readable at the application layer.

---

## Problem

Production backends generate large volumes of logs, but modern observability tooling still leaves engineers doing the hard work:

- Logs are noisy and unstructured.
- Recurring failures are buried in volume.
- Engineers manually correlate errors across services and time windows.
- There is no built-in layer that groups, prioritizes, or explains failures.

As a result, incident detection and triage stay slow, reactive, and expensive.

---

## What TraceRoot Does

TraceRoot converts raw logs into a structured incident stream:

1. Logs are ingested, validated, and normalized.
2. ERROR logs are fingerprinted on `(serviceName, level, exceptionType, endpoint)`.
3. Fingerprint matches within a time window are counted.
4. Once a threshold is crossed, an incident is created.
5. Subsequent matching logs update the incident's event count and last-seen timestamp.
6. Incidents pass through a lifecycle: ACTIVE → RESOLVED, with automatic reopening if the same pattern recurs within a configured window.
7. An AI summarization layer generates structured summaries, probable causes, and recommended checks for each incident.

---

## Architecture

### Platform (modular monolith)
ingestion/   → log intake, validation, normalization, persistence
search/      → log querying and filtering
incident/    → fingerprinting, detection, lifecycle, reopening
ai/          → prompt construction, LLM client, structured summary generation
metric/      → incident metrics and analytics aggregations

Each module has a single responsibility and communicates through explicit service interfaces.

### Failure Lab (distributed sandbox)
api-gateway      → entry point, simulates top-level failure propagation
order-service    → orchestrates inventory + payment, drives retry storms
inventory-service → simulates null pointers, dependency outages, stale stock
payment-service   → simulates timeouts, provider outages, transient failures

Each service emits structured logs to the TraceRoot platform via a shared ingestion client. Failure flags propagate from the gateway through the call chain, producing realistic cross-service error patterns — for example, a payment retry storm generates bursts of logs from payment-service (ERROR), order-service (WARN per retry + ERROR on exhaustion), and api-gateway (ERROR on propagation).

### Planned: polyglot storage

The platform currently uses PostgreSQL for all storage. The roadmap splits storage by access pattern:

- **PostgreSQL** retains incidents, lifecycle state, summaries, metrics, and configuration.
- **OpenSearch** handles raw log indexing, full-text search, faceted filtering, and time-series aggregations.

This split reflects that transactional incident state and high-volume log search are fundamentally different workloads.

---

## Key Features

### Incident detection and lifecycle

- Fingerprint-based pattern matching on four fields (service, level, exception type, endpoint).
- Threshold-based incident creation (default: 3 matching errors within a 5-minute window).
- Automatic incident reopening when a resolved pattern recurs within 24 hours.
- Event count and last-seen tracking on active incidents.

### AI summarization

- Structured prompt builder with explicit rules, JSON-only output, and controlled context size.
- Summary, probable cause, and recommended checks generated per incident.
- Persisted summaries with staleness detection — summaries are marked stale when the underlying incident changes and regenerated on next request.
- LLM client is interface-based; currently using a stub for development, designed for drop-in replacement with OpenAI, Anthropic, or self-hosted models.

### Metrics and analytics

- Active, resolved, and total incident counts.
- 24-hour rolling incident creation and resolution rates.
- Average resolution time.
- Per-service incident breakdown (total, active, resolved).
- Top recurring incident patterns across the system.

### Failure Lab

- Four independently deployable Spring Boot services.
- Configurable failure modes per call (null pointers, dependency failures, timeouts, provider outages, retry storms).
- Realistic retry storm simulation — repeated calls driven by the order service produce time-spread error bursts, not single fake log loops.
- Shared log client emits structured events to the TraceRoot ingestion endpoint.

---

## API Endpoints

**Logs**
- `POST /api/logs` — ingest a structured log event
- `GET /api/logs` — filtered log search (by service, level)
- `GET /api/logs/{id}` — fetch single log

**Incidents**
- `GET /api/incidents` — list all incidents (newest first)
- `GET /api/incidents/{id}` — fetch incident detail
- `GET /api/incidents/{id}/logs` — logs matching an incident's fingerprint
- `GET /api/incidents/{id}/summary` — AI-generated summary (persisted, regenerated when stale)
- `POST /api/incidents/{id}/resolve` — mark resolved

**Metrics**
- `GET /api/metrics/incidents` — platform-wide incident metrics
- `GET /api/metrics/incidents/services` — per-service breakdown
- `GET /api/metrics/incidents/top-patterns` — top recurring fingerprints

---

## Tech Stack

- Java 17
- Spring Boot 3
- PostgreSQL
- JPA / Hibernate
- Jackson (structured JSON handling)
- Bean Validation
- OpenSearch (planned)

---

## Running Locally

1. Clone the repository.
2. Start PostgreSQL (local or Docker).
3. Configure `application.properties` with your database connection.
4. Run `TraceRootApplication` to start the platform.
5. Run each Failure Lab service independently (`ApiGatewayApplication`, `OrderServiceApplication`, `InventoryServiceApplication`, `PaymentServiceApplication`) to generate realistic failure traffic.
6. Trigger scenarios with `curl` or Postman against the gateway at `/checkout`.

Example — trigger a payment retry storm:

```bash
curl -X POST http://localhost:8080/checkout \
  -H "Content-Type: application/json" \
  -d '{
    "traceId": "demo-1",
    "orderId": "order-001",
    "sku": "SKU-42",
    "quantity": 1,
    "amount": 29.99,
    "simulatePaymentRetryStorm": true
  }'
```

Then query `GET /api/incidents` to see the resulting incident, and `GET /api/incidents/{id}/summary` for the AI-generated summary.

---

## Design Principles

- **Deterministic detection before AI involvement.** Fingerprinting, thresholds, and lifecycle are all rule-based. AI only explains — it does not decide.
- **Separation of ingestion and query concerns.** Writing logs and reading logs are different problems and are modeled separately.
- **Staleness over regeneration.** Summaries are persisted and regenerated only when the underlying incident changes — balancing cost and freshness.
- **Realistic failure simulation.** The Failure Lab produces real distributed error patterns, not synthetic log data. This makes detection tuning meaningful rather than hypothetical.
- **Polyglot storage where justified.** PostgreSQL for transactional state, OpenSearch for log volume — each tool used for what it does best.

---

## Roadmap

- OpenSearch integration for high-volume log indexing and search.
- Real LLM integration with retries, timeouts, and fallback behavior.
- Evaluation harness for measuring AI summary quality.
- React frontend for incident dashboards, metrics visualization, and summary review.
- Incident timeline / activity feed endpoint.
- Alerting and notification integrations (Slack, email, PagerDuty-style).
- Anomaly detection beyond threshold rules (error rate spikes, cross-service correlation).

---

## Why This Project Matters

TraceRoot demonstrates backend engineering beyond CRUD APIs: observability patterns, lifecycle modeling, structured AI integration, and distributed systems simulation. The combination of a production-shaped platform *and* a realistic failure generator is intentional — it's what separates "I built a tool" from "I built a tool and stress-tested it against real distributed traffic."

---

## Author

Built by Ozioma Ochin. Backend + AI systems work focused on reliability, observability, and intelligent debugging workflows.

Writing about the design decisions behind TraceRoot at https://ozi.hashnode.dev.