# IT Incident Intelligence Platform (ITII)

An AI-powered incident response system built with **Java 17**, **Spring Boot 3.3**,
**Spring AI 1.1.0** and the **Model Context Protocol (MCP)**.

> **Best practices** — note the spelling! (not "practises" for the noun form in software engineering context)

---

## The Idea

Production incidents are expensive. Engineers spend valuable on-call hours manually:
- Searching log aggregation systems for error patterns
- Querying metrics dashboards for resource anomalies
- Looking up runbooks for known failure modes
- Cross-referencing past incidents for precedents
- Writing post-mortem reports

**ITII automates this investigation loop.** When an incident is reported, three AI agents
orchestrate a structured investigation using MCP tools, then present the engineer with a
root-cause hypothesis, evidence trail and step-by-step remediation recommendations.

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                       Client (curl / Postman)                │
└─────────────────────────────┬───────────────────────────────┘
                               │ HTTP REST
┌─────────────────────────────▼───────────────────────────────┐
│              incident-agent-app  (port 8080)                  │
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │                 REST Controllers                         │ │
│  │   IncidentController    DemoController                   │ │
│  └──────────────────────────┬──────────────────────────────┘ │
│                              │                                │
│  ┌───────────────────────────▼──────────────────────────────┐ │
│  │             IncidentOrchestrator (Agent Pipeline)        │ │
│  │                                                          │ │
│  │   ┌─────────────────┐    ┌──────────────────────────┐   │ │
│  │   │  TriageAgent    │    │    DiagnosisAgent         │   │ │
│  │   │                 │    │                           │   │ │
│  │   │ - Severity      │    │ - Root cause analysis     │   │ │
│  │   │ - Category      │    │ - Evidence correlation    │   │ │
│  │   │ - Affected svcs │    │ - Runbook matching        │   │ │
│  │   └────────┬────────┘    └──────────────┬────────────┘   │ │
│  └────────────┼─────────────────────────────┼───────────────┘ │
│               │    MCP Client (HTTP/SSE)     │                 │
│               └──────────────┬──────────────┘                 │
└──────────────────────────────┼─────────────────────────────────┘
                                │ MCP Protocol (HTTP/SSE)
┌──────────────────────────────▼─────────────────────────────────┐
│              incident-mcp-server  (port 8081)                   │
│                                                                  │
│  MCP Tools exposed via HTTP/SSE:                                 │
│  ┌──────────────────┐  ┌──────────────────┐                     │
│  │  search_logs     │  │ get_svc_metrics  │                     │
│  └──────────────────┘  └──────────────────┘                     │
│  ┌──────────────────┐  ┌──────────────────┐                     │
│  │ search_runbooks  │  │ get_incident_    │                     │
│  │                  │  │ history          │                     │
│  └──────────────────┘  └──────────────────┘                     │
│  ┌──────────────────┐  ┌──────────────────┐                     │
│  │ get_svc_status   │  │ add_incident_    │                     │
│  │                  │  │ note             │                     │
│  └──────────────────┘  └──────────────────┘                     │
└──────────────────────────────┬──────────────────────────────────┘
                                │ JDBC
┌──────────────────────────────▼──────────────────────────────────┐
│                     PostgreSQL 16                                 │
│  incidents  incident_notes  services  runbooks                   │
│  simulated_logs  simulated_metrics                               │
└──────────────────────────────────────────────────────────────────┘
```

### What is MCP (Model Context Protocol)?

MCP is an open standard (by Anthropic) that defines how AI applications discover and
call external tools. Think of it as a **USB standard for AI tools**:

- The **MCP Server** advertises a catalogue of callable tools
- The **MCP Client** connects to the server, fetches the tool catalogue, and makes tools
  available to the LLM
- When the LLM wants to call a tool, the client forwards the call to the server and
  returns the result to the LLM
- The LLM can call multiple tools in a single inference session

This decoupling means the same MCP server can be reused by many different AI applications,
and tools can be developed, versioned and deployed independently of the AI agents.

---

## Project Structure

```
pekelund-ai/
├── pom.xml                          Parent POM (dependency management)
│
├── incident-mcp-server/             MCP Server module
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/dev/pekelund/ai/mcp/
│       ├── IncidentMcpServerApplication.java
│       ├── config/McpServerConfig.java       Tool registration
│       ├── domain/                           JPA entities (owns full schema)
│       ├── repository/                       Spring Data repositories
│       └── tool/                             @Tool-annotated MCP tool implementations
│           ├── LogQueryTool.java
│           ├── MetricsQueryTool.java
│           ├── RunbookSearchTool.java
│           ├── IncidentHistoryTool.java
│           └── ServiceStatusTool.java
│
├── incident-agent-app/              Agent Application module
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/dev/pekelund/ai/agent/
│       ├── IncidentAgentApplication.java
│       ├── config/AgentConfig.java           ChatClient + MCP wiring
│       ├── agent/
│       │   ├── TriageAgent.java              Severity/category classification
│       │   ├── DiagnosisAgent.java           Root-cause analysis
│       │   └── IncidentOrchestrator.java     Pipeline coordinator
│       ├── domain/                           JPA entities (validates schema)
│       ├── dto/                              Request/Response records
│       ├── repository/                       Spring Data repositories
│       ├── service/IncidentService.java      Business logic
│       └── controller/
│           ├── IncidentController.java       /api/incidents REST API
│           └── DemoController.java           /api/demo pre-built scenarios
│
└── docker-compose.yml               Orchestrates all three services
```

---

## Prerequisites

| Tool        | Version   | Purpose                  |
|-------------|-----------|--------------------------|
| Docker      | 20.10+    | Container runtime        |
| Docker Compose | v2+   | Service orchestration    |
| OpenAI API key | -      | LLM for AI agents        |
| Java 17+    | (optional)| For local development    |
| Maven 3.9+  | (optional)| For local development    |

---

## Quick Start (Docker)

```bash
# 1. Clone the repository
git clone https://github.com/pekelund-dev/pekelund-ai.git
cd pekelund-ai

# 2. Set your OpenAI API key
export OPENAI_API_KEY=sk-your-key-here

# 3. Build and start all services
docker compose up --build

# Wait ~2 minutes for everything to start. You will see:
#   ✓ PostgreSQL ready
#   ✓ MCP server started, Flyway migrations applied, tools registered
#   ✓ Agent app started, connected to MCP server

# 4. Run a demo scenario (in a new terminal)
curl -X POST "http://localhost:8080/api/demo/simulate?scenario=payment-db"
```

---

## API Reference

All endpoints are on `http://localhost:8080`.

### Incidents

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/incidents` | Report a new incident |
| `GET` | `/api/incidents` | List all incidents |
| `GET` | `/api/incidents/{id}` | Get incident + notes |
| `PUT` | `/api/incidents/{id}/status` | Update status |
| `POST` | `/api/incidents/{id}/notes` | Add manual note |
| `POST` | `/api/incidents/{id}/analyse` | **Trigger AI analysis** |

### Demo

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/demo/simulate?scenario=payment-db` | Run pre-built scenario |
| `POST` | `/api/demo/scenarios` | List available scenarios |

### Available Demo Scenarios

| Scenario | Description |
|----------|-------------|
| `payment-db` | Payment service database connection exhaustion |
| `fraud-memory` | Fraud service OOM kill after deployment |
| `gateway-down` | API Gateway returning 503/429 for all traffic |

---

## Example Workflow

### 1. Report a new incident

```bash
curl -X POST http://localhost:8080/api/incidents \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Payment service returning 503 errors",
    "description": "Users are unable to complete checkout. Error rate at 80%. Started 5 minutes ago.",
    "reportedBy": "on-call-engineer"
  }'
# Response: {"id": 4, "status": "OPEN", "message": "Call POST /api/incidents/4/analyse ..."}
```

### 2. Trigger AI analysis

```bash
curl -X POST http://localhost:8080/api/incidents/4/analyse
# Runs triage + diagnosis + summary (30–120 seconds)
# Returns: severity, category, root cause, recommendations, runbook reference
```

### 3. Get full incident report

```bash
curl http://localhost:8080/api/incidents/4
# Returns incident details + all AI-generated timeline notes
```

---

## Key Design Decisions

### Why two separate modules?

MCP is designed for **service separation**. By running the MCP server as a distinct
service:
- Tools can be updated/deployed independently of the AI agents
- The same tool server could serve multiple agent applications
- Tool implementations are isolated from AI logic

### Why `gpt-4o-mini`?

Balances cost and capability for this demo. The structured output format in the agent
system prompts compensates for the reduced reasoning depth. Upgrading to `gpt-4o` in
`application.yml` improves analysis quality.

### Why synchronous MCP (SYNC mode)?

The SYNC transport is simpler to reason about and sufficient for an incident management
use case where requests are infrequent and tool latency is acceptable. For high-throughput
scenarios, switch to ASYNC mode in both server and client configuration.

### Why structured output format in prompts?

Rather than using JSON mode or structured output APIs, the agents use a rigid
`FIELD: value` format. This is:
- Easy to parse deterministically with the `parseField` helper
- Human-readable in logs and notes
- Compatible with any LLM without format-specific API dependencies

---

## Local Development

```bash
# Start just PostgreSQL
docker compose up postgres -d

# Run MCP server
cd incident-mcp-server
mvn spring-boot:run

# Run agent app (in a new terminal)
cd incident-agent-app
OPENAI_API_KEY=sk-your-key mvn spring-boot:run

# Run tests (no external services needed — uses H2 and mocks)
mvn test
```

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `OPENAI_API_KEY` | *(required)* | OpenAI API key |
| `DB_URL` | `jdbc:postgresql://localhost:5432/itii_db` | JDBC connection URL |
| `DB_USERNAME` | `itii` | Database username |
| `DB_PASSWORD` | `itii_secret` | Database password |
| `MCP_SERVER_URL` | `http://localhost:8081` | MCP server base URL |

---

## Learning Resources

This project demonstrates several advanced Spring AI and MCP concepts:

| Concept | Where to look |
|---------|---------------|
| MCP Server setup | `McpServerConfig.java`, `application.yml` (mcp-server) |
| `@Tool` annotation | All files in `incident-mcp-server/tool/` |
| MCP Client config | `application.yml` (agent-app), `AgentConfig.java` |
| AI Agent with tool calls | `TriageAgent.java`, `DiagnosisAgent.java` |
| Agent orchestration | `IncidentOrchestrator.java` |
| System prompts | System prompt constants in each agent class |
| Structured output parsing | `IncidentOrchestrator.parseField()` |
| Spring AI ChatClient | `AgentConfig.java` |
