# IT Incident Intelligence Platform (ITII)

An AI-powered incident response system built with **Java 17**, **Spring Boot 4.0**,
**Spring AI 2.0.0-M2**, the **Model Context Protocol (MCP)** and the **Agent-to-Agent (A2A) Protocol**.

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

### What is A2A (Agent-to-Agent Protocol)?

A2A is an open standard (by Google, 2025) that defines how AI agents discover and talk to
**each other** — regardless of vendor, framework or programming language. Think of it as the
**HTTP for agent interoperability**:

- Every A2A agent publishes an **Agent Card** at `GET /.well-known/agent-card.json` describing
  its skills, input/output formats and task endpoint
- Clients POST JSON messages to the agent's task endpoint (`POST /a2a`)
- The agent executes the task and returns a structured **Task** response with status and
  content parts
- Agents can call **other agents** as sub-tasks, enabling true multi-agent collaboration

**ITII uses [`spring-ai-a2a-server-autoconfigure`](https://github.com/spring-ai-community/spring-ai-a2a) (Spring AI 2.0.0-M2)**,
which auto-configures all A2A endpoints from two user-provided beans: `AgentCard` and `AgentExecutor`.

#### ITII A2A Endpoints

All A2A and REST endpoints are under the `/a2a` servlet context-path (required so the A2A
`MessageController` at `POST /` doesn't conflict with Spring Boot's default routes).

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/a2a/.well-known/agent-card.json` | `GET` | Agent card — lists skills and task URL (primary discovery) |
| `/a2a/card` | `GET` | Alternative agent card endpoint |
| `/a2a` | `POST` | JSON message send (A2A `sendMessage`) |
| `/a2a/tasks/{id}` | `GET` | Task status retrieval |
| `/a2a/tasks/{id}/cancel` | `POST` | Cancel a running task |

#### A2A Skills

| Skill ID | Description |
|----------|-------------|
| `incident_analysis` | Full pipeline: triage + root-cause diagnosis + summary (default) |
| `incident_triage`   | Fast triage only — severity, category, affected services |

Pass `"skillId": "incident_triage"` in the request `metadata` map to select a skill.

#### Example A2A Request

```bash
# Discover the agent card
curl http://localhost:8080/a2a/.well-known/agent-card.json

# Submit a task (full analysis — default skill)
curl -X POST http://localhost:8080/a2a \
  -H "Content-Type: application/json" \
  -d '{
    "id": "req-1",
    "message": {
      "role": "user",
      "parts": [{"type": "text", "text": "title: Payment 503\ndescription: Error rate at 80% since 5 minutes ago"}]
    }
  }'

# Submit a fast triage task (pass skillId in metadata)
curl -X POST http://localhost:8080/a2a \
  -H "Content-Type: application/json" \
  -d '{
    "id": "req-2",
    "metadata": {"skillId": "incident_triage"},
    "message": {
      "role": "user",
      "parts": [{"type": "text", "text": "title: API gateway 503\ndescription: All endpoints down since 15 minutes ago"}]
    }
  }'

# Get task status
curl http://localhost:8080/a2a/tasks/{taskId}
```

---


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
│       ├── config/
│       │   ├── AgentConfig.java              ChatClient + MCP wiring
│       │   └── A2AConfig.java                AgentCard + AgentExecutor beans (A2A)
│       ├── TriageAgent.java                  Severity/category classification
│       ├── DiagnosisAgent.java               Root-cause analysis
│       ├── IncidentOrchestrator.java         Pipeline coordinator
│       ├── domain/                           JPA entities (validates schema)
│       ├── dto/                              Request/Response records
│       ├── repository/                       Spring Data repositories
│       ├── service/IncidentService.java      Business logic
│       └── controller/
│           ├── IncidentController.java       /a2a/api/incidents REST API
│           └── DemoController.java           /a2a/api/demo pre-built scenarios
│
└── docker-compose.yml               Orchestrates all three services
```

---

## Prerequisites

| Tool        | Version   | Purpose                  |
|-------------|-----------|--------------------------|
| Docker      | 20.10+    | Container runtime        |
| Docker Compose | v2+   | Service orchestration    |
| Gemini API key | -      | LLM for AI agents (free at [ai.google.dev](https://ai.google.dev)) |
| Java 17+    | (optional)| For local development    |
| Maven 3.9+  | (optional)| For local development    |

---

## Quick Start (Docker)

```bash
# 1. Clone the repository
git clone https://github.com/pekelund-dev/pekelund-ai.git
cd pekelund-ai

# 2. Set your Gemini API key (free at https://ai.google.dev/)
export GEMINI_API_KEY=AIza-your-key-here

# 3. Build and start all services
docker compose up --build

# Wait ~2 minutes for everything to start. You will see:
#   ✓ PostgreSQL ready
#   ✓ MCP server started, Flyway migrations applied, tools registered
#   ✓ Agent app started, connected to MCP server

# 4. Run a demo scenario (in a new terminal)
curl -X POST "http://localhost:8080/a2a/api/demo/simulate?scenario=payment-db"
```

---

## API Reference

All endpoints are on `http://localhost:8080`. All paths are under the `/a2a` servlet context-path.

### Incidents

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/a2a/api/incidents` | Report a new incident |
| `GET` | `/a2a/api/incidents` | List all incidents |
| `GET` | `/a2a/api/incidents/{id}` | Get incident + notes |
| `PUT` | `/a2a/api/incidents/{id}/status` | Update status |
| `POST` | `/a2a/api/incidents/{id}/notes` | Add manual note |
| `POST` | `/a2a/api/incidents/{id}/analyse` | **Trigger AI analysis** |

### A2A Protocol

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/a2a/.well-known/agent-card.json` | Agent card — primary A2A discovery endpoint |
| `GET` | `/a2a/card` | Agent card — alternative endpoint |
| `POST` | `/a2a` | A2A message send (`sendMessage`) |
| `GET` | `/a2a/tasks/{id}` | Task status retrieval |
| `POST` | `/a2a/tasks/{id}/cancel` | Cancel a running task |

### Demo

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/a2a/api/demo/simulate?scenario=payment-db` | Run pre-built scenario |
| `POST` | `/a2a/api/demo/scenarios` | List available scenarios |

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
curl -X POST http://localhost:8080/a2a/api/incidents \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Payment service returning 503 errors",
    "description": "Users are unable to complete checkout. Error rate at 80%. Started 5 minutes ago.",
    "reportedBy": "on-call-engineer"
  }'
# Response: {"id": 4, "status": "OPEN", "message": "Call POST /a2a/api/incidents/4/analyse ..."}
```

### 2. Trigger AI analysis

```bash
curl -X POST http://localhost:8080/a2a/api/incidents/4/analyse
# Runs triage + diagnosis + summary (30–120 seconds)
# Returns: severity, category, root cause, recommendations, runbook reference
```

### 3. Get full incident report

```bash
curl http://localhost:8080/a2a/api/incidents/4
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

### Why `spring-ai-a2a-server-autoconfigure` instead of a custom implementation?

Spring AI 2.0.0-M2 is the first Spring AI version with production-grade A2A support
via the community library [`spring-ai-a2a`](https://github.com/spring-ai-community/spring-ai-a2a).
By using it, all A2A protocol complexity (JSON-RPC serialisation, task store, queue
management, push notification hooks) is handled by the framework. You only provide:
1. An `AgentCard` bean describing your agent's skills
2. An `AgentExecutor` bean that processes incoming tasks

This is the same "framework over boilerplate" philosophy as Spring MVC vs. raw Servlets.



Google's Gemini 2.0 Flash offers excellent reasoning at low cost and has a generous free
tier via the [Gemini Developer API](https://ai.google.dev/). For deeper analysis, switch
to `gemini-1.5-pro` in `application.yml`. The structured output format in the agent system
prompts means any capable Gemini model works well.

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
GEMINI_API_KEY=AIza-your-key mvn spring-boot:run

# Run tests (no external services needed — uses H2 and mocks)
mvn test
```

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `GEMINI_API_KEY` | *(required)* | Google Gemini API key (get free at [ai.google.dev](https://ai.google.dev)) |
| `DB_URL` | `jdbc:postgresql://localhost:5432/itii_db` | JDBC connection URL |
| `DB_USERNAME` | `itii` | Database username |
| `DB_PASSWORD` | `itii_secret` | Database password |
| `MCP_SERVER_URL` | `http://localhost:8081` | MCP server base URL |

---

## Learning Resources

This project demonstrates several advanced Spring AI, MCP and A2A concepts:

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
| **A2A Agent Card** | `A2AConfig.agentCard()` — `io.a2a.spec.AgentCard` |
| **A2A Agent Executor** | `A2AConfig.agentExecutor()` — `DefaultAgentExecutor` |
| **A2A framework library** | `spring-ai-a2a-server-autoconfigure:0.2.0` |
| **Gemini integration** | `application.yml` (`spring.ai.google.genai`) |
