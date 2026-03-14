# IT Incident Intelligence Platform — Architecture & Learning Guide

This document is a deep-dive into every layer of the system. It is intended as a
guided learning resource: you can read it top-to-bottom to understand how the pieces
fit together, or jump to any section that interests you.

---

## Table of Contents

1. [Big Picture](#1-big-picture)
2. [Technology Stack](#2-technology-stack)
3. [Repository Layout](#3-repository-layout)
4. [Component Deep-Dives](#4-component-deep-dives)
   - 4.1 [incident-mcp-server](#41-incident-mcp-server)
   - 4.2 [incident-agent-app](#42-incident-agent-app)
5. [The MCP Protocol — How Tools Reach the LLM](#5-the-mcp-protocol--how-tools-reach-the-llm)
6. [The A2A Protocol — Agent Discovery & Interoperability](#6-the-a2a-protocol--agent-discovery--interoperability)
7. [The AI Pipeline — Step-by-Step Request Flow](#7-the-ai-pipeline--step-by-step-request-flow)
8. [Database Schema](#8-database-schema)
9. [Key Spring AI Concepts Demonstrated](#9-key-spring-ai-concepts-demonstrated)
10. [Learning Path: Follow Along with Curl](#10-learning-path-follow-along-with-curl)
11. [Configuration Reference](#11-configuration-reference)
12. [FAQ / Design Decisions](#12-faq--design-decisions)

---

## 1. Big Picture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    IT Incident Intelligence Platform                        │
│                                                                             │
│  External caller (curl / A2A client / your browser)                        │
│            │                                                                │
│            ▼  HTTP                                                          │
│  ┌─────────────────────┐        MCP / SSE        ┌──────────────────────┐  │
│  │  incident-agent-app │ ──────────────────────► │ incident-mcp-server  │  │
│  │      port 8080      │ ◄────── JSON results ── │      port 8081       │  │
│  │                     │                         │                      │  │
│  │  TriageAgent        │                         │  search_logs         │  │
│  │  DiagnosisAgent     │                         │  get_service_metrics │  │
│  │  IncidentOrchest.   │                         │  search_runbooks     │  │
│  │  A2A endpoints      │                         │  get_incident_history│  │
│  │  REST API           │                         │  add_incident_note   │  │
│  └──────────┬──────────┘                         │  get_service_status  │  │
│             │ HTTPS                              └──────────┬───────────┘  │
│             ▼                                               │              │
│    Google Gemini API                                        │ SQL / JPA    │
│    (gemini-2.0-flash)                              ┌────────▼──────────┐   │
│                                                    │   PostgreSQL 16   │   │
│                                                    │    itii_db        │   │
│                                                    └───────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

**In plain English:**

- A caller (human or another AI agent) submits an incident description.
- The **agent-app** orchestrates three AI agents that investigate the incident.
- Each agent uses the **ChatClient** (Spring AI) to call Google Gemini.
- When Gemini wants data (logs, metrics, runbooks …), it issues *tool calls*.
- The **MCP client** inside the agent-app translates those tool calls into HTTP
  requests to the **mcp-server**, which queries PostgreSQL and returns JSON.
- The agents build up evidence and produce a structured analysis report.
- The whole thing can also be accessed by external AI agents via the **A2A protocol**.

---

## 2. Technology Stack

| Layer | Technology | Purpose |
|---|---|---|
| Language | Java 21 | Virtual threads, records, sealed classes |
| Framework | Spring Boot 4.0.3 | Application container, auto-configuration |
| AI Framework | Spring AI 2.0.0-M2 | ChatClient, MCP client/server, A2A |
| LLM | Google Gemini `gemini-2.0-flash` | The actual "brain" — reasoning over evidence |
| Tool Protocol | MCP (Model Context Protocol) | Structured tool-call standard over HTTP/SSE |
| Agent Protocol | A2A (Agent-to-Agent) 0.3.0 | Agent discovery and interoperability |
| Database | PostgreSQL 16 | Persistent storage for incidents, logs, metrics |
| Schema Mgmt | Flyway | Versioned SQL migrations |
| Build | Maven (multi-module) | Dependency management and packaging |
| Containers | Docker + Compose | Local orchestration |
| Serialization | Jackson 3.x (Spring Boot) + Jackson 2.x (MCP SDK) | See §12 |

---

## 3. Repository Layout

```
incident-intelligence/
├── pom.xml                          ← Parent POM: versions, BOM imports
├── docker-compose.yml               ← Orchestrates postgres + mcp-server + agent-app
├── ARCHITECTURE.md                  ← This file
│
├── incident-mcp-server/             ← Module 1: operational toolbox
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/dev/pekelund/mcp/
│       ├── IncidentMcpServerApplication.java   Spring Boot entry point
│       ├── config/McpServerConfig.java         Tool registration
│       ├── domain/                             JPA entities (owns the full schema)
│       │   ├── Incident.java
│       │   ├── IncidentNote.java
│       │   ├── Runbook.java
│       │   ├── ServiceEntity.java
│       │   ├── SimulatedLog.java
│       │   └── SimulatedMetric.java
│       ├── repository/                         Spring Data JPA repositories
│       └── tool/                               @Tool-annotated MCP tool implementations
│           ├── LogQueryTool.java               search_logs
│           ├── MetricsQueryTool.java           get_service_metrics
│           ├── RunbookSearchTool.java          search_runbooks
│           ├── IncidentHistoryTool.java        get_incident_history, add_incident_note
│           └── ServiceStatusTool.java          get_service_status
│
└── incident-agent-app/              ← Module 2: AI agents + REST + A2A
    ├── pom.xml
    ├── Dockerfile
    └── src/main/java/dev/pekelund/agent/
        ├── IncidentAgentApplication.java       Spring Boot entry point
        ├── TriageAgent.java                    Phase 1: classify severity/category
        ├── DiagnosisAgent.java                 Phase 2: root-cause investigation
        ├── IncidentOrchestrator.java           Coordinates the pipeline
        ├── config/
        │   ├── AgentConfig.java               ChatClient wiring with MCP tools
        │   └── A2AConfig.java                 A2A Agent Card + AgentExecutor
        ├── controller/
        │   ├── IncidentController.java         REST CRUD + /analyse endpoint
        │   └── DemoController.java             Pre-built demo scenarios
        ├── domain/                             JPA entities (schema-validate only)
        ├── dto/                                Request/Response records
        ├── repository/                         Spring Data JPA repositories
        └── service/IncidentService.java        Business logic layer
```

---

## 4. Component Deep-Dives

### 4.1 `incident-mcp-server`

**Responsibility:** Expose operational data (logs, metrics, runbooks, service registry,
incident history) as MCP tools over HTTP/SSE so that AI agents can query them at
inference time.

#### Startup sequence

```
1. Spring Boot starts on port 8081
2. Flyway runs V1__create_tables.sql  → creates all 6 tables
3. Flyway runs V2__seed_data.sql      → inserts 8 services, 5 runbooks, ~100 log
                                        entries, ~50 metric samples, 5 historic incidents
4. McpServerConfig registers tool beans via MethodToolCallbackProvider
5. Spring AI MCP Server starter exposes:
     GET  /sse           → SSE stream (clients subscribe here)
     POST /mcp/message   → tool-call request endpoint
6. Server ready — waiting for MCP client connections
```

#### The 6 MCP Tools

| Tool Name | Class | What It Queries | Key Use |
|---|---|---|---|
| `get_service_status` | `ServiceStatusTool` | `services` table | What services exist? Who owns them? What do they depend on? |
| `search_logs` | `LogQueryTool` | `simulated_logs` | Find ERROR/WARN bursts and stack traces |
| `get_service_metrics` | `MetricsQueryTool` | `simulated_metrics` | CPU, memory, error rate, p95 latency |
| `search_runbooks` | `RunbookSearchTool` | `runbooks` | Find documented remediation procedures |
| `get_incident_history` | `IncidentHistoryTool` | `incidents` | Similar past incidents and how they were fixed |
| `add_incident_note` | `IncidentHistoryTool` | `incident_notes` | Persist AI analysis as timeline notes |

#### How `@Tool` registration works

```java
// Tool class (simplified)
@Component
public class LogQueryTool {

    @Tool(name = "search_logs", description = "Search application logs...")
    public String searchLogs(
            @ToolParam(description = "Name of the service") String serviceName,
            @ToolParam(description = "Log level filter")   String level,
            @ToolParam(description = "Substring to match") String pattern,
            @ToolParam(description = "Max results")        int limit) {
        // ... query DB, return JSON string
    }
}

// Config class
@Bean
public ToolCallbackProvider incidentToolCallbackProvider(
        LogQueryTool logQueryTool, /* ... */) {
    return MethodToolCallbackProvider.builder()
            .toolObjects(logQueryTool, /* ... */)
            .build();
}
```

`MethodToolCallbackProvider` uses reflection to:
1. Find every method annotated with `@Tool`
2. Build a JSON Schema from the method's parameters (using `@ToolParam` descriptions)
3. Register each method as a callable tool that the MCP server will advertise

When the LLM issues a tool call, the MCP server uses the same reflection to invoke the
method with the LLM-provided arguments and return the result as a string.

---

### 4.2 `incident-agent-app`

**Responsibility:** Host the AI agents, orchestrate the analysis pipeline, persist
incidents, and expose a REST API and A2A endpoints.

#### Startup sequence

```
1. Spring Boot starts on port 8080 (context-path: /a2a)
2. Flyway runs V1__placeholder.sql  → no-op; ensures Flyway history table exists
3. Hibernate validates schema       → confirms expected tables are present
4. MCP client connects to mcp-server:8081/sse
5. ToolCallbackProvider created with all 6 remote tools
6. ChatClient built with defaultTools(toolCallbackProvider)
7. A2A autoconfigure creates all A2A endpoints
8. Server ready
```

#### The three AI agents

```
TriageAgent
  Purpose    : Fast severity/category classification
  Pattern    : Single-turn ChatClient call with tool use
  Tools used : get_service_status, search_logs
  Output     : Structured text — SEVERITY, CATEGORY, AFFECTED_SERVICES, TEAM,
               CONFIDENCE, REASONING

DiagnosisAgent
  Purpose    : Deep root-cause investigation
  Pattern    : Multi-turn ChatClient call (LLM drives multiple tool calls)
  Tools used : All 6 tools
  Output     : Structured text — ROOT_CAUSE, CONTRIBUTING_FACTORS, EVIDENCE,
               RECOMMENDED_RUNBOOK, BLAST_RADIUS, CONFIDENCE, REASONING

IncidentOrchestrator
  Purpose    : Pipeline coordinator + executive summary synthesis
  Pattern    : Calls TriageAgent, then DiagnosisAgent, then a synthesis
               ChatClient call (no tools — pure text synthesis)
  Persists   : Triage note, diagnosis note, executive summary note in DB
  Output     : AnalysisResult record
```

#### `AgentConfig` — wiring tools into ChatClient

```java
@Bean
public ChatClient chatClient(ChatClient.Builder builder,
                             Optional<ToolCallbackProvider> toolCallbackProvider) {
    ChatClient.Builder configured = builder;
    if (toolCallbackProvider.isPresent()) {
        configured = configured.defaultTools(toolCallbackProvider.get());
    }
    return configured.build();
}
```

`Optional<ToolCallbackProvider>` is used so this bean compiles and runs when the MCP
client is disabled (e.g., in tests). In production the `ToolCallbackProvider` is always
present — it is auto-created by the `spring-ai-starter-mcp-client` based on the
connections declared in `application.yml`.

#### REST API endpoints (all under `/a2a` context path)

| Method | Path | Description |
|---|---|---|
| `POST` | `/a2a/api/incidents` | Create a new incident |
| `GET` | `/a2a/api/incidents` | List all incidents |
| `GET` | `/a2a/api/incidents/{id}` | Get incident + timeline notes |
| `PUT` | `/a2a/api/incidents/{id}/status` | Update status |
| `POST` | `/a2a/api/incidents/{id}/notes` | Add a manual note |
| `POST` | `/a2a/api/incidents/{id}/analyse` | **Trigger AI analysis** |
| `POST` | `/a2a/api/demo/simulate?scenario=...` | Run a pre-built demo |

---

## 5. The MCP Protocol — How Tools Reach the LLM

MCP (Model Context Protocol) is an open standard for connecting AI models to external
data and tools over HTTP. Think of it as a typed RPC layer between an LLM and the
real world.

### Transport: HTTP/SSE

```
agent-app (MCP client)              mcp-server (MCP server)
        │                                    │
        │── GET /sse ──────────────────────► │  (subscribe to SSE stream)
        │◄─── SSE: tool list ──────────────  │  (server sends available tools)
        │                                    │
        │── POST /mcp/message ─────────────► │  (tool-call request)
        │   { tool: "search_logs",           │
        │     args: { serviceName: "...", …} }│
        │◄─── SSE: tool result ────────────  │  (server responds via SSE stream)
        │   { content: "[{...}]" }           │
```

### Tool-call loop (managed by Spring AI `ChatClient`)

```
ChatClient.prompt().user("Triage this incident…").call()
    │
    ▼
[1] Send user message + tool definitions to Gemini
    │
    ▼  Gemini response: "I need to check logs. TOOL_CALL: search_logs(payment-service, ERROR, …)"
    │
[2] ChatClient detects tool call, invokes MCP client
    │
    ▼
[3] MCP client sends POST /mcp/message to mcp-server
    │
    ▼  mcp-server queries DB, returns JSON log entries via SSE
    │
[4] ChatClient appends tool result to conversation context
    │
    ▼
[5] Send updated context back to Gemini
    │
    ▼  Gemini response: "SEVERITY: HIGH\nCATEGORY: DATABASE\n…"  (no more tool calls)
    │
[6] ChatClient returns final text response to the agent
```

Steps [2]–[5] repeat for each tool call the LLM makes. The `TriageAgent` typically
makes 2 tool calls; the `DiagnosisAgent` typically makes 4–6.

### Why SSE instead of REST?

SSE (Server-Sent Events) is a unidirectional HTTP stream. The client subscribes once
and the server pushes responses when ready. This avoids polling and allows the server
to stream partial results. MCP 1.0 uses SSE for the response channel while keeping
HTTP POST for the request channel (a common pattern for avoiding WebSocket complexity).

---

## 6. The A2A Protocol — Agent Discovery & Interoperability

A2A is a 2025 open standard by Google that lets AI agents discover and call each other
over plain HTTP + JSON. It is analogous to OpenAPI for REST APIs: the Agent Card is
the machine-readable description, and the task endpoint is where you send work.

### How agents discover each other

```
External A2A client
    │
    │── GET /a2a/.well-known/agent-card.json ──►  agent-app
    │                                              Returns AgentCard JSON:
    │◄── AgentCard ─────────────────────────────  {
    │    name: "ITII Incident Analysis Agent"        "name": "...",
    │    url:  "http://localhost:8080/a2a/"           "url": "...",
    │    skills: [incident_analysis,                  "skills": [...],
    │             incident_triage]                    ...
    │                                              }
```

### Sending a task

```
External A2A client
    │
    │── POST /a2a ──────────────────────────────►  agent-app
    │   {                                           DefaultAgentExecutor picks up task
    │     "id": "req-1",                            Runs IncidentOrchestrator or TriageAgent
    │     "message": {                              Returns result via A2A task response
    │       "role": "user",
    │       "parts": [{"type": "text",
    │                  "text": "title: Payment 503\ndescription: Error rate 80%"}]
    │     }
    │   }
    │
    │◄── A2A Task Response ──────────────────────
    │   {
    │     "id": "req-1",
    │     "status": {"state": "completed"},
    │     "artifacts": [{
    │       "parts": [{"type": "text", "text": "## Incident Analysis Report\n..."}]
    │     }]
    │   }
```

### Skill routing

Send `metadata.skillId` to select which skill handles the task:

```json
{
  "id": "req-1",
  "message": { "role": "user", "parts": [{ "type": "text", "text": "title: …" }] },
  "metadata": { "skillId": "incident_triage" }
}
```

| `skillId` | System Prompt Used | What Happens |
|---|---|---|
| `incident_analysis` (default) | `ANALYSIS_SYSTEM_PROMPT` | Full 6-tool investigation + structured report |
| `incident_triage` | `TRIAGE_SYSTEM_PROMPT` | Fast 2-tool triage only |

### Framework integration

The `spring-ai-a2a-server-autoconfigure:0.2.0` library provides all the A2A
infrastructure. The application only defines two beans in `A2AConfig.java`:

```java
@Bean
public AgentCard agentCard(...) { /* self-description */ }

@Bean
public AgentExecutor agentExecutor(ChatClient chatClient) {
    return new DefaultAgentExecutor(chatClient, (chat, ctx) -> {
        // skill routing + ChatClient call
    });
}
```

The library auto-configures five endpoints, a task store, and the full A2A message
lifecycle (submitted → working → completed/failed).

---

## 7. The AI Pipeline — Step-by-Step Request Flow

This section traces a single incident from API call to completed analysis.

### Entry point: Demo API

```bash
POST /a2a/api/demo/simulate?scenario=payment-db
```

`DemoController` creates a `CreateIncidentRequest` with a pre-written title and
description, then delegates to `IncidentService`.

### Step 1 — Persist the incident

```
IncidentService.createIncident(request)
    │
    ▼
Incident saved to DB with status = "OPEN"
    │
    ▼
Return saved Incident (has a DB-assigned ID now, e.g. id=42)
```

The incident must be persisted before analysis starts because the tool
`add_incident_note` needs a valid `incidentId` to attach notes to. Creating first
also means the incident appears in the REST API immediately, even before analysis.

### Step 2 — Start the orchestrator

```
IncidentService.analyse(42)
    │
    ▼
IncidentOrchestrator.analyse(incident)
```

The orchestrator is a `@Transactional` Spring component. It owns the pipeline
sequencing, error handling, and persistence of results.

### Step 3 — Phase 1: Triage

```
TriageAgent.triage(incident)
    │
    ▼
ChatClient.prompt()
    .system(SYSTEM_PROMPT)           ← "You are an expert SRE performing rapid triage…"
    .user("INCIDENT ID: 42\n        ← Incident title + description
           TITLE: Payment Service: Customers unable to complete purchases\n
           DESCRIPTION: Users are reporting that checkout is failing with error 503…")
    .call()
    │
    ▼  [Tool call 1] Gemini: "I need to check which services exist"
       → get_service_status(serviceName="")
       ← JSON: [{name:"payment-service", team:"Payments Team", critical:true, …}, …]
    │
    ▼  [Tool call 2] Gemini: "I should check recent errors in payment-service"
       → search_logs(serviceName="payment-service", level="ERROR", pattern="", limit=20)
       ← JSON: [{level:"ERROR", message:"Unable to acquire JDBC Connection", …}, …]
    │
    ▼  Final text response:
       "SEVERITY: HIGH
        CATEGORY: DATABASE
        AFFECTED_SERVICES: payment-service,order-service
        TEAM: Payments Team
        CONFIDENCE: HIGH
        REASONING: High error rate with connection exhaustion errors in payment-service
                   logs indicates a database connectivity issue. Order service depends
                   on payment and will also be impacted."
```

The orchestrator then calls `parseField()` to extract each value and updates the
incident record in the DB (severity, category, affected_services, status → IN_PROGRESS).

### Step 4 — Phase 2: Diagnosis

```
DiagnosisAgent.diagnose(incident)
    │
    ▼
ChatClient.prompt()
    .system(SYSTEM_PROMPT)           ← "You are a senior infrastructure engineer…"
    .user("INCIDENT ID: 42\n         ← Now includes severity + affected_services
           SEVERITY: HIGH\n             from the triage step
           AFFECTED_SERVICES: payment-service,order-service\n
           CATEGORY: DATABASE\n…")
    .call()
    │
    ▼  [Tool call 1] → search_logs(payment-service, ERROR, connection, 20)
       ← JSON log entries with "Unable to acquire JDBC Connection" + timestamps
    │
    ▼  [Tool call 2] → get_service_metrics(payment-service, "", 10)
       ← p95_latency_ms=6500, error_rate_per_min=72, cpu_percent=45
    │
    ▼  [Tool call 3] → get_incident_history("payment-service", "DATABASE", 5)
       ← [{title:"Payment DB pool exhaustion — Dec 2024",
           rootCause:"Hikari pool size too small after traffic spike",
           resolution:"Increased pool size + added slow-query index"}, …]
    │
    ▼  [Tool call 4] → search_runbooks("connection pool", "payment-service", 3)
       ← [{title:"Payment Service: Database Connection Exhaustion",
           content:"## Symptoms\n- Unable to acquire JDBC…\n## Resolution\n1. Restart…"}]
    │
    ▼  [Tool call 5] → add_incident_note(42, "## Initial finding: connection pool …", "DiagnosisAgent")
       ← {success:true, noteId:7, …}
    │
    ▼  Final text response:
       "ROOT_CAUSE: The payment-service Hikari connection pool is exhausted due to
                   a long-running transaction introduced in the v3.2.0 deployment.
        CONTRIBUTING_FACTORS:
          1. v3.2.0 introduced a transaction that holds DB connections for 30+ seconds
          2. Hikari pool size (10) insufficient for current traffic
          3. No circuit breaker between order-service and payment-service
        EVIDENCE:
          - LOGS: 47 ERROR entries 'Unable to acquire JDBC Connection' in last 5 minutes
          - METRICS: p95_latency_ms=6500ms (normal: 120ms), error_rate=72/min
          - HISTORY: Identical pattern in Dec 2024 incident — resolved by pool increase
        RECOMMENDED_RUNBOOK: Payment Service: Database Connection Exhaustion
        BLAST_RADIUS: payment-service (primary), order-service (dependent, degraded)
        CONFIDENCE: HIGH
        REASONING: The metric and log correlation is unambiguous…"
```

### Step 5 — Phase 3: Executive Summary

```
IncidentOrchestrator.synthesiseExecutiveSummary(incident, triageResult, diagnosisResult)
    │
    ▼
ChatClient.prompt()
    .user("You are writing an executive summary…
           TRIAGE: <triageResult>
           DIAGNOSIS: <diagnosisResult>")
    .call()
    │         ← NOTE: No tools in this step — synthesis from already-gathered evidence
    ▼
"The payment service is experiencing a database connectivity failure affecting all
 checkout operations, causing a full revenue impact since 14:32. Root cause is a
 connection pool exhaustion triggered by a transaction leak introduced in the v3.2.0
 deployment 40 minutes ago. Immediate action: restart payment-service pods and roll
 back v3.2.0, then consult the connection pool runbook for permanent fix."
```

### Step 6 — Persist and return

```
incident.aiSummary = executiveSummary  ─► save to DB
incident.rootCause = rootCause         ─► save to DB

noteRepository.save(triageNote)          ─► "## Triage Assessment\n…"    (author: TriageAgent)
noteRepository.save(diagnosisNote)       ─► "## Root Cause Analysis\n…"  (author: DiagnosisAgent)
noteRepository.save(summaryNote)         ─► "## Executive Summary\n…"    (author: IncidentOrchestrator)

return AnalysisResult(
    incidentId=42,
    severity="HIGH",
    category="DATABASE",
    affectedServices="payment-service,order-service",
    rootCause="The payment-service Hikari connection pool is exhausted…",
    executiveSummary="The payment service is experiencing a database connectivity failure…",
    recommendedSteps="1. Restart the payment-service pod…",
    runbookReference="Payment Service: Database Connection Exhaustion",
    confidence="HIGH"
)
```

---

## 8. Database Schema

All tables are created by Flyway migration `V1__create_tables.sql` and owned by
`incident-mcp-server`. The `incident-agent-app` connects to the same database but
only validates (never modifies) the schema.

```
services                         runbooks
─────────────────                ─────────────────────────────
id           BIGSERIAL PK        id           BIGSERIAL PK
name         VARCHAR(100) UQ     title        VARCHAR(255)
description  TEXT                description  TEXT
team         VARCHAR(100)        service_name VARCHAR(100)
dependencies VARCHAR(500)        content      TEXT         ← full runbook steps
critical     BOOLEAN             tags         VARCHAR(500)
created_at   TIMESTAMP           created_at   TIMESTAMP
                                 updated_at   TIMESTAMP

simulated_logs                   simulated_metrics
──────────────────               ──────────────────────────
id           BIGSERIAL PK        id           BIGSERIAL PK
service_name VARCHAR(100)        service_name VARCHAR(100)
level        VARCHAR(20)         metric_name  VARCHAR(100)
message      TEXT                value        NUMERIC(15,4)
metadata     TEXT (JSON)         unit         VARCHAR(50)
timestamp    TIMESTAMP           timestamp    TIMESTAMP
  INDEX (service_name, timestamp DESC)    INDEX (service_name, timestamp DESC)

incidents                        incident_notes
──────────────────────────────   ─────────────────────────
id               BIGSERIAL PK    id           BIGSERIAL PK
title            VARCHAR(255)    incident_id  BIGINT FK → incidents.id
description      TEXT            content      TEXT
severity         VARCHAR(20)     note_type    VARCHAR(20)  ← "AUTO" | "MANUAL"
status           VARCHAR(30)     author       VARCHAR(100) ← agent name or human
category         VARCHAR(100)    created_at   TIMESTAMP
affected_services VARCHAR(500)
reported_by      VARCHAR(100)
root_cause       TEXT            ← set by DiagnosisAgent
ai_summary       TEXT            ← set by IncidentOrchestrator
resolution       TEXT
created_at       TIMESTAMP
resolved_at      TIMESTAMP
```

**Seed data (V2__seed_data.sql) includes:**
- 8 services (api-gateway, payment-service, order-service, fraud-service, …)
- 5 runbooks with full remediation steps
- ~100 simulated log entries (ERRORs, WARNs, INFOs for realistic agent responses)
- ~50 metric samples across services
- 5 pre-resolved historic incidents (for `get_incident_history` tool)

---

## 9. Key Spring AI Concepts Demonstrated

### 9.1 ChatClient

The central Spring AI abstraction. Wraps the LLM provider and handles the
request/response lifecycle including tool-call resolution.

```java
// Simple call (no tools)
String result = chatClient.prompt()
        .system("You are an SRE…")
        .user("Triage this incident: " + description)
        .call()
        .content();

// Call with tools pre-wired (set in AgentConfig via defaultTools())
// The ChatClient automatically resolves tool calls before returning
```

### 9.2 `@Tool` annotation

Turns a Java method into an LLM-callable tool. Spring AI generates a JSON Schema from
the parameter types and `@ToolParam` descriptions, which is sent to the LLM in the
system context so it knows when and how to call the tool.

### 9.3 `MethodToolCallbackProvider`

Scans a set of Spring beans for `@Tool`-annotated methods and produces a
`ToolCallbackProvider` that the MCP server can register.

```java
MethodToolCallbackProvider.builder()
        .toolObjects(logQueryTool, metricsQueryTool, /* … */)
        .build();
```

### 9.4 MCP Client auto-configuration

Adding `spring-ai-starter-mcp-client` to `pom.xml` and configuring a connection URL
in `application.yml` is all that is needed to create a `ToolCallbackProvider` that
proxies tool calls over the wire to a remote MCP server:

```yaml
spring.ai.mcp.client:
  sse.connections:
    incident-mcp-server:
      url: http://mcp-server:8081
  toolcallback.enabled: true
```

### 9.5 A2A AutoConfigure

`spring-ai-a2a-server-autoconfigure:0.2.0` auto-configures all A2A endpoints from
two user beans: `AgentCard` and `AgentExecutor`. The `DefaultAgentExecutor` receives
the `ChatClient` and a lambda that defines the actual AI logic.

### 9.6 Deterministic output parsing

Rather than using JSON mode (which can be brittle across LLMs), the system uses
structured plain-text output:

```
SEVERITY: HIGH
CATEGORY: DATABASE
AFFECTED_SERVICES: payment-service,order-service
```

`IncidentOrchestrator.parseField()` extracts fields by line prefix. This works
reliably across different LLMs and temperature settings, and is easy to read in logs.

---

## 10. Learning Path: Follow Along with Curl

Start the system with:

```bash
cd incident-intelligence
export GEMINI_API_KEY=AIza-your-key
docker compose up --build
# Wait ~2 minutes for all services to start
```

### Step 1 — Verify the system is up

```bash
# Check the agent-app health
curl http://localhost:8080/a2a/actuator/health

# Check the MCP server health
curl http://localhost:8081/actuator/health
```

Expected: `{"status":"UP"}` from both.

### Step 2 — Explore the A2A Agent Card

This is the machine-readable self-description of the agent — the first thing an A2A
client would request to understand what the agent can do.

```bash
curl http://localhost:8080/a2a/.well-known/agent-card.json | jq .
```

You should see the agent's name, description, task URL, and two skills
(`incident_analysis`, `incident_triage`).

### Step 3 — Create an incident manually

```bash
curl -X POST http://localhost:8080/a2a/api/incidents \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Payment Service: High error rate",
    "description": "Users cannot complete checkout. Error rate is 85%. Started 10 minutes ago.",
    "reportedBy": "monitoring-bot"
  }'
```

Note the `id` in the response (e.g. `1`). The incident is `OPEN` with no severity yet.

### Step 4 — Trigger AI analysis and watch the magic

```bash
curl -X POST http://localhost:8080/a2a/api/incidents/1/analyse
```

This call takes 30–90 seconds. While it runs, open the agent-app logs
(`docker compose logs -f agent-app`) and observe:

- `TriageAgent starting triage for incident 1`
- MCP client log lines showing tool calls (`search_logs`, `get_service_status`)
- `DiagnosisAgent starting investigation for incident 1`
- Multiple tool calls with their results
- `Orchestrator completed full analysis for incident 1`

The response will be a structured `AnalysisResult` JSON.

### Step 5 — Read the full incident with AI notes

```bash
curl http://localhost:8080/a2a/api/incidents/1 | jq .
```

You should see the incident with updated severity/category/rootCause, and a `notes`
array containing three AUTO notes: one from TriageAgent, one from DiagnosisAgent, and
one executive summary from IncidentOrchestrator.

### Step 6 — Use the A2A protocol directly

This demonstrates the inter-agent protocol, as if another AI agent were calling this one:

```bash
# Full analysis via A2A
curl -X POST http://localhost:8080/a2a \
  -H "Content-Type: application/json" \
  -d '{
    "id": "a2a-req-1",
    "message": {
      "role": "user",
      "parts": [{"type": "text",
                 "text": "title: Fraud service OOM\ndescription: Pod crashing every 90 min since v2.4.1 deployment"}]
    }
  }'
```

```bash
# Fast triage only — add skillId in metadata
curl -X POST http://localhost:8080/a2a \
  -H "Content-Type: application/json" \
  -d '{
    "id": "a2a-req-2",
    "message": {
      "role": "user",
      "parts": [{"type": "text", "text": "title: API Gateway 503\ndescription: All traffic failing"}]
    },
    "metadata": { "skillId": "incident_triage" }
  }'
```

### Step 7 — Run a pre-built demo scenario

For the most realistic demo with elaborately crafted seed data:

```bash
# Options: payment-db, fraud-memory, gateway-down
curl -X POST "http://localhost:8080/a2a/api/demo/simulate?scenario=fraud-memory" | jq .
```

The response includes both the full `AnalysisResult` and the complete `Incident`
object with all AI-generated timeline notes.

### Step 8 — Resolve the incident

```bash
curl -X PUT http://localhost:8080/a2a/api/incidents/1/status \
  -H "Content-Type: application/json" \
  -d '{"status": "RESOLVED"}'
```

The `resolvedAt` timestamp is automatically set.

---

## 11. Configuration Reference

### `incident-agent-app/src/main/resources/application.yml`

| Property | Default | Description |
|---|---|---|
| `server.port` | `8080` | HTTP port |
| `server.servlet.context-path` | `/a2a` | All endpoints prefixed with `/a2a` |
| `spring.ai.google.genai.api-key` | `${GEMINI_API_KEY}` | Gemini API key — must be set |
| `spring.ai.google.genai.chat.options.model` | `gemini-2.0-flash` | LLM model |
| `spring.ai.google.genai.chat.options.temperature` | `0.2` | Low = more deterministic |
| `spring.ai.mcp.client.sse.connections.incident-mcp-server.url` | `http://localhost:8081` | MCP server URL |
| `spring.ai.a2a.server.enabled` | `true` | Enable A2A endpoints |
| `DB_URL` | `jdbc:postgresql://localhost:5432/itii_db` | DB connection |

### `incident-mcp-server/src/main/resources/application.yml`

| Property | Default | Description |
|---|---|---|
| `server.port` | `8081` | HTTP port |
| `spring.ai.mcp.server.name` | `incident-tools-mcp-server` | Server identity advertised to clients |
| `spring.ai.mcp.server.type` | `SYNC` | Synchronous tool execution |
| `spring.ai.mcp.server.sse-message-endpoint` | `/mcp/message` | Tool call POST endpoint |
| `spring.flyway.enabled` | `true` | Run schema migrations on startup |

### Environment variables (Docker Compose)

| Variable | Required | Description |
|---|---|---|
| `GEMINI_API_KEY` | **Yes** | Your Google Gemini API key from [ai.google.dev](https://ai.google.dev) |
| `DB_URL` | No | Defaults to `jdbc:postgresql://postgres:5432/itii_db` |
| `DB_USERNAME` | No | Defaults to `itii` |
| `DB_PASSWORD` | No | Defaults to `itii_secret` |
| `MCP_SERVER_URL` | No | Defaults to `http://mcp-server:8081` |

---

## 12. FAQ / Design Decisions

**Q: Why two separate Spring Boot applications instead of one?**

Separation of concerns. The MCP server is a pure toolbox — it has no LLM dependency,
no API keys, and could be used by any MCP-compatible agent. The agent-app is the
"brain" that knows how to reason. This separation makes it easier to swap the LLM
provider (e.g. swap Gemini for OpenAI) without touching the tool layer.

**Q: Why is `ToolCallbackProvider` declared as `Optional`?**

So the `ChatClient` bean can be created in test mode when the MCP client is disabled
(`spring.ai.mcp.client.enabled=false` in `application-test.yml`). Without `Optional`,
the application context would fail to start in tests because there would be no
`ToolCallbackProvider` bean. With `Optional`, the ChatClient is created without tools
in test mode, which is fine because tests mock the agents directly.

**Q: Why is there an explicit Jackson 2.x `ObjectMapper` bean?**

Spring Boot 4.x upgraded its default Jackson to version 3.x (`tools.jackson.*`
package namespace). The Spring AI MCP SDK and several related libraries still use
Jackson 2.x (`com.fasterxml.jackson.*`). Both versions coexist safely (different
Java packages), but we need to explicitly provide a `com.fasterxml.jackson.databind.ObjectMapper`
bean so the tool classes can inject it. Spring Boot 4.x's auto-configured `ObjectMapper`
is a Jackson 3.x instance and doesn't satisfy the Jackson 2.x injection point.

**Q: Why not use JSON mode for LLM output?**

JSON mode requires the LLM to produce valid JSON at every token, which can cause
generation failures or unexpected escaping with some models/versions. The plain-text
`FIELD: value` format is easier to debug (readable in logs), works consistently across
temperature settings, and is trivial to parse with `parseField()`. The tradeoff is
that the parser is slightly more fragile for multi-line values, but in practice the
strict system prompt keeps the LLM on format.

**Q: Why is the orchestrator's executive-summary step done without tools?**

The synthesis step takes the already-gathered evidence (triage result + diagnosis
result) and produces a management-level narrative. There is no new data to fetch — all
facts are in the context. Restricting tools in this step avoids non-determinism from
additional tool calls and makes the summary faster and more focused.

**Q: Why does the agent-app also have Flyway configured?**

The agent-app uses a *separate* Flyway history table (`flyway_schema_history_agent`)
and only has a no-op placeholder migration (`V1__placeholder.sql`). This is needed
because Flyway's `baseline-on-migrate: true` setting requires a migration to exist.
The real schema is managed entirely by the mcp-server's Flyway migrations. The
agent-app uses `hibernate.ddl-auto=validate` to confirm at startup that the expected
tables are present.

**Q: Why `gemini-2.0-flash` instead of a larger model?**

`gemini-2.0-flash` is fast and free-tier eligible. The structured-output approach
(plain text with field prefixes) does not require heavy reasoning — the prompts are
explicit and the tool results are concrete. For more complex incidents or higher
confidence requirements, change the model to `gemini-1.5-pro` in `application.yml`
with no other code changes required.
