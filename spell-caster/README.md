# 🔮 Spell Caster — Distributed Multi-Agent System

> A fantasy-themed distributed Multi-Agent System that demonstrates three distinct agent execution
> patterns — orchestrated by an LLM-powered Summoner.

**Tech stack:** Java 25 · Spring Boot 4.0.3 · Spring AI 2.0.0-M2 · Gemini 2.0 Flash

---

## Table of Contents

1. [What is this?](#what-is-this)
2. [Does it use A2A?](#does-it-use-a2a)
3. [Architecture overview](#architecture-overview)
4. [Module map](#module-map)
5. [Communication protocol: Agent Cards](#communication-protocol-agent-cards)
6. [Execution patterns at a glance](#execution-patterns-at-a-glance)
7. [Governance layer: AOP cooldowns](#governance-layer-aop-cooldowns)
8. [Short-term memory](#short-term-memory)
9. [Web UI](#web-ui)
10. [Prerequisites](#prerequisites)
11. [Quick start](#quick-start)
12. [Configuration reference](#configuration-reference)
13. [API reference](#api-reference)
14. [Request flow walkthrough](#request-flow-walkthrough)
15. [Testing](#testing)
16. [Design decisions](#design-decisions)

---

## What is this?

Spell Caster is a demonstration of a **hierarchical multi-agent system** where:

- A central **Summoner** agent receives natural-language commands and uses a Gemini LLM to decide which subordinate agent ("familiar") to invoke.
- Three **Familiars** (Fire, Water, Earth) are independent microservices, each showcasing a different execution pattern: sequential, parallel, and iterative.
- A **governance layer** (Spring AOP) enforces cooldown rules between familiar calls without touching the agent's own logic.
- A browser-based **Web UI** (Thymeleaf + HTMX) lets you watch the execution flow in real time.

The system is inspired by the Google ADK / MCP / A2A architecture demonstrated in the video  
[Build a Multi-Agent System with ADK, MCP, and Gemini](https://youtu.be/2ERrxG-Ii3I), but re-implemented **exclusively with Java 25 and Spring Boot 4** — no Python, no MCP, no A2A.

---

## Does it use A2A?

**No.** This system does _not_ use the Agent-to-Agent (A2A) protocol.

Instead it implements a **lightweight, custom REST-based service-discovery protocol**:

| Concern | A2A approach | This implementation |
|---------|-------------|---------------------|
| Service discovery | `.well-known/agent-card.json` (A2A spec) | `GET /agent-card` (custom JSON) |
| Task invocation | JSON-RPC 2.0 over POST `/a2a` | Plain HTTP POST `/execute` |
| Task lifecycle | Async task state machine (`tasks/{id}`) | Synchronous request/response |
| Protocol library | `spring-ai-a2a-server-autoconfigure:0.2.0` | None — plain `RestClient` |

This is a **deliberate simplification**: the goal is to teach how multi-agent routing works at the Java/Spring AI level without adding the complexity of a full A2A runtime. Other modules in this repository (such as `stock-winner`) do use the A2A protocol if you want to see that pattern.

---

## Architecture overview

```
┌─────────────────────────────────────────────────────────┐
│                    User / Browser                        │
│                  http://localhost:8090                   │
└────────────────────────┬────────────────────────────────┘
                         │ POST /ui/summon  (HTMX form)
                         │ or POST /summon  (REST API)
                         ▼
┌─────────────────────────────────────────────────────────┐
│                     THE SUMMONER                         │
│                    (port 8090)                           │
│                                                          │
│  ┌──────────────┐  ┌─────────────┐  ┌───────────────┐  │
│  │  Gemini LLM  │  │ ChatMemory  │  │  CooldownAOP  │  │
│  │  (routing)   │  │ (20 msgs)   │  │  (@Aspect)    │  │
│  └──────┬───────┘  └─────────────┘  └───────────────┘  │
│         │ routes to one of:                              │
└─────────┼───────────────────────────────────────────────┘
          │
    ┌─────┴──────────────────────┐
    │                            │
    ▼                            ▼                         ▼
┌──────────────┐     ┌──────────────────┐     ┌──────────────────┐
│  FIRE        │     │  WATER FAMILIAR  │     │  EARTH FAMILIAR  │
│  FAMILIAR    │     │  (port 8092)     │     │  (port 8093)     │
│  (port 8091) │     │                  │     │                  │
│              │     │  CryoShatter ──┐ │     │  SeismicCharge   │
│  SpellSearch │     │                ├─┼──▶  │  (loop until     │
│      ↓       │     │  Moonlight     │ │     │   threshold)     │
│  Amplify     │     │  Cascade ──────┘ │     │                  │
│              │     │  (StructuredTask │     │                  │
│  (Sequential)│     │   Scope, Java25) │     │  (Iterative)     │
└──────────────┘     └──────────────────┘     └──────────────────┘
    Sequential              Parallel               Iterative
```

---

## Module map

| Module | Port | Pattern | Key technology |
|--------|------|---------|----------------|
| [`summoner`](summoner/README.md) | 8090 | Orchestrator | Gemini LLM, Spring AI ChatClient, AOP, Web UI |
| [`familiar-fire`](familiar-fire/README.md) | 8091 | Sequential | Spring Data JPA, H2 (Librarium DB) |
| [`familiar-water`](familiar-water/README.md) | 8092 | Parallel (fan-out/fan-in) | `StructuredTaskScope` (Java 25) |
| [`familiar-earth`](familiar-earth/README.md) | 8093 | Iterative (loop) | Deterministic computation |

All four modules share the same Maven parent (`spell-caster/pom.xml`) and are built together.

---

## Communication protocol: Agent Cards

Every familiar service exposes two endpoints:

### `GET /agent-card`

Returns a self-description of the familiar's capabilities. The Summoner fetches this at startup (`@PostConstruct`) to build its routing table.

Example response from `familiar-fire`:

```json
{
  "name": "fire-familiar",
  "displayName": "Ignis — Fire Familiar",
  "description": "Sequential fire agent that scouts the Librarium database for a spell and amplifies it.",
  "endpoint": "http://localhost:8091/execute",
  "skills": [
    {
      "name": "fire_attack",
      "description": "Scout the Librarium for a fire spell and amplify it with Ignis Amplification Protocol."
    }
  ]
}
```

### `POST /execute`

Accepts `{ "target": "<string>" }` and returns the familiar's result.

This intentionally simple contract means any HTTP client can call a familiar directly — no special library needed.

---

## Execution patterns at a glance

### 🔥 Fire Familiar — Sequential (Scout → Amplify)

```
User command
    │
    ▼
SpellSearchTool.scoutSpell(element)
    │  queries H2 Librarium DB for most powerful spell of that element
    ▼
AmplifyTool.amplify(spellResult)
    │  multiplies power by 1.5x with Ignis Amplification Protocol
    ▼
Result
```

The output of Step 1 is fed directly into Step 2. Neither step can start until the previous completes.

### 🌊 Water Familiar — Parallel (CryoShatter ∥ MoonlightCascade → PowerMerge)

```
User command
    │
    ├──────────────────────────────────┐
    │                                  │
    ▼                                  ▼
CryoShatterTool.cryoShatter()    MoonlightCascadeTool.moonlightCascade()
    │  virtual thread #1               │  virtual thread #2
    │  (Nexus channel)                 │  (Forge channel)
    │                                  │
    └─────────────┬────────────────────┘
                  │  StructuredTaskScope.join() — blocks until BOTH complete
                  ▼
             powerMerger(nexusResult, forgeResult)
                  │
                  ▼
               Result
```

Both tools run simultaneously on virtual threads managed by `StructuredTaskScope.open(allSuccessfulOrThrow())`. If either throws, the scope cancels the other and propagates the exception.

### 🌍 Earth Familiar — Iterative (SeismicCharge loop)

```
User command
    │
    ▼  totalEnergy = 0, iteration = 0
    │
    ├─▶ seismicChargeTool.seismicCharge(iteration)
    │       deterministic formula: 15.0 + (iteration × 2.5) + random(0..10)
    │   totalEnergy += charge
    │
    └─▶ while totalEnergy < 100.0 AND iteration < 10
    │
    ▼
Release attack (threshold reached or max iterations)
    │
    ▼
Result (charge log + outcome + totals)
```

---

## Governance layer: AOP cooldowns

The `CooldownAspect` in the Summoner enforces that the same familiar cannot be called again within 60 seconds.

```
Summoner.summon(command)
    │
    ▼ LLM picks "fire-familiar"
    │
    ▼ SummonerService.invokeFire(endpoint, target)
         │
         ▼ @CooldownProtected(familiarName = "fire-familiar", cooldownSeconds = 60)
              │
              ▼ CooldownAspect.enforceCooldown() [Before any REST call]
                   ├── lastCall exists AND (now - lastCall) < 60s ?
                   │       └── BLOCK → return cooldown message (familiar never contacted)
                   └── else
                           ├── record lastCall = now
                           └── PROCEED → callFamiliar(endpoint, target)
```

Key design decisions:
- **AOP not conditional logic** — The governance rule lives entirely outside the agent's service code. Removing or changing it requires no modification to `SummonerService`.
- **Injectable `Clock`** — `CooldownAspect` takes a `java.time.Clock` bean. Tests inject a `MutableClock` to advance time without sleeping.
- **`ConcurrentHashMap`** — Cooldown state is thread-safe; Virtual Thread workloads can call the aspect concurrently.

---

## Short-term memory

The Summoner uses Spring AI's `MessageChatMemoryAdvisor` with an in-memory repository, retaining the last 20 messages per session.

```java
ChatMemory chatMemory = MessageWindowChatMemory.builder()
    .chatMemoryRepository(new InMemoryChatMemoryRepository())
    .maxMessages(20)
    .build();

ChatClient chatClient = ChatClient.builder(chatModel)
    .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
    .build();
```

Memory is keyed by `sessionId` (a UUID supplied by the caller — or generated by the UI from `localStorage`). The Gemini system prompt instructs the LLM to check memory before choosing a familiar:

> _"Do NOT summon the same familiar twice in a row (check memory)."_

The combination of memory + instruction implements the "rotation" rule without any imperative code checking previous state.

---

## Web UI

A Thymeleaf + HTMX dashboard is served at `http://localhost:8090/`.

```
┌─────────────────────────────────────────────────────────────────┐
│  🔮 The Summoner   Multi-Agent Spell Casting System             │
├─────────────────┬───────────────────────────────────────────────┤
│  ⚡ Familiars   │  🪄 Cast a Spell                              │
│                 │  ┌─────────────────────────────┐ [⚡ Summon]  │
│  🔥 Ignis       │  │ Attack the dragon with fire │             │
│  🟢 Ready       │  └─────────────────────────────┘             │
│  Sequential     │                                               │
│  Port 8091      │  📜 Execution Flow                            │
│                 │  📨 Command received: "Attack the dragon..."  │
│  🌊 Aqua        │  🧠 Querying Gemini LLM for routing...        │
│  🟡 Cooldown 42s│  🔀 LLM decision: fire-familiar|dragon        │
│  Parallel       │  🎯 Selected: fire-familiar · Target: dragon  │
│  Port 8092      │  📡 Calling familiar endpoint: .../execute    │
│                 │  ✅ Familiar responded successfully.           │
│  🌍 Terra       │                                               │
│  🔴 Offline     │  ✨ Spell Result                              │
│  Iterative      │  🔥 AMPLIFIED [x1.5]: Scout found: Inferno    │
│  Port 8093      │  Blast (power=95)...                          │
└─────────────────┴───────────────────────────────────────────────┘
```

### HTMX interactions

| Action | HTMX trigger | Target | Behaviour |
|--------|-------------|--------|-----------|
| Page load | `hx-trigger="load"` | `#familiars-panel` | Fetch familiar status from `GET /ui/familiars` |
| Auto-refresh | `hx-trigger="every 5s"` | `#familiars-panel` | Poll familiar status (cooldown countdown) |
| Cast spell | Form submit | `#result-area` | POST to `/ui/summon`, swap result |
| Flow events | OOB swap | `#flow-panel` | Execution steps updated in same response |
| Cooldown update | OOB swap | `#familiars-panel` | Sidebar refreshed immediately after cast |

Session IDs are persisted in `localStorage` so the Summoner's LLM conversation memory (and "don't repeat" rule) survives page reloads.

---

## Prerequisites

| Requirement | Notes |
|-------------|-------|
| Java 25 | `--enable-preview` required for `StructuredTaskScope` in `familiar-water` |
| Maven 3.9+ | Tested with 3.9.x |
| Gemini API key | Required only by `summoner`. Familiars have no LLM dependency. |

---

## Quick start

### 1. Build

```bash
cd spell-caster
mvn clean package -DskipTests
```

> **Note:** `familiar-water` uses `StructuredTaskScope` which is a preview API in Java 25.  
> The Maven build has `--enable-preview` configured; just ensure your `JAVA_HOME` points to Java 25.

### 2. Start the familiars (in separate terminals)

```bash
# Terminal 1 — Fire Familiar (port 8091)
cd familiar-fire
mvn spring-boot:run

# Terminal 2 — Water Familiar (port 8092)
cd familiar-water
mvn spring-boot:run

# Terminal 3 — Earth Familiar (port 8093)
cd familiar-earth
mvn spring-boot:run
```

Familiars have no external dependencies (no API keys required).  
Fire Familiar auto-seeds its H2 Librarium database on startup.

### 3. Start the Summoner

```bash
export GEMINI_API_KEY=your-key-here
cd summoner
mvn spring-boot:run
```

The Summoner will:
1. Fetch `GET /agent-card` from each familiar at startup.
2. Log discovered familiars and their skills.
3. Start the web server on port 8090.

### 4. Open the Web UI

Navigate to **http://localhost:8090/** in a browser.

Try commands like:
- `Attack the dragon with fire!`
- `Cast a water spell on the troll`
- `Charge up a seismic attack against the golem`
- `Use ice magic against the phoenix`

### 5. Use the REST API directly

```bash
# Summon via REST
curl -X POST http://localhost:8090/summon \
  -H "Content-Type: application/json" \
  -d '{"command": "Attack the dragon with fire", "sessionId": "my-session"}'

# List discovered familiars
curl http://localhost:8090/summon/familiars

# Call a familiar directly (bypasses Summoner + AOP)
curl -X POST http://localhost:8091/execute \
  -H "Content-Type: application/json" \
  -d '{"target": "fire"}'
```

---

## Configuration reference

### Environment variables

| Variable | Module | Required | Description |
|----------|--------|----------|-------------|
| `GEMINI_API_KEY` | summoner | ✅ Yes | Gemini API key for LLM routing |
| `FAMILIAR_FIRE_URL` | summoner | ❌ No | Override fire URL (default: `http://localhost:8091`) |
| `FAMILIAR_WATER_URL` | summoner | ❌ No | Override water URL (default: `http://localhost:8092`) |
| `FAMILIAR_EARTH_URL` | summoner | ❌ No | Override earth URL (default: `http://localhost:8093`) |

### application.yml properties

The Summoner (`summoner/src/main/resources/application.yml`):

```yaml
spring.ai.google.genai.api-key: ${GEMINI_API_KEY}
spring.ai.google.genai.chat.options.model: gemini-2.0-flash
spring.ai.google.genai.chat.options.temperature: 0.2
spring.threads.virtual.enabled: true   # Enables virtual threads globally

summoner.familiars.fire-url: http://localhost:8091
summoner.familiars.water-url: http://localhost:8092
summoner.familiars.earth-url: http://localhost:8093
```

---

## API reference

### Summoner (port 8090)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/` | Web UI dashboard (Thymeleaf) |
| `POST` | `/summon` | Execute a summon command (JSON API) |
| `GET` | `/summon/familiars` | List discovered familiars |
| `GET` | `/ui/familiars` | HTMX fragment: familiar status cards |
| `POST` | `/ui/summon` | HTMX endpoint: summon + return HTML fragments |

### Familiars (ports 8091 / 8092 / 8093)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/agent-card` | Returns the familiar's Agent Card (discovery) |
| `POST` | `/execute` | Executes the familiar's attack pattern |

Fire Familiar also exposes:

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/h2-console` | H2 web console for the Librarium database |

---

## Request flow walkthrough

Here is a complete trace for the command `"Attack the dragon with fire!"`:

```
1. Browser POST /ui/summon  {command: "Attack the dragon with fire!", sessionId: "abc123"}

2. WebController.summon()
   └─▶ SummonerService.summon("Attack the dragon with fire!", "abc123")

3. FlowTracker.track(COMMAND) — "📨 Command received"

4. AgentDiscoveryService.getDiscoveredAgents()
   └─ returns [fire-familiar, water-familiar, earth-familiar] (discovered at startup)

5. FlowTracker.track(ROUTING) — "🧠 Querying Gemini LLM..."

6. ChatClient.prompt()
       .system("You are The Summoner... Available familiars: ...")
       .user("Attack the dragon with fire!")
       .advisors(chat_memory_conversation_id="abc123")
       .call().content()
   └─ LLM responds: "fire-familiar|dragon"

7. FlowTracker.track(ROUTING) — "🔀 LLM decision: fire-familiar|dragon"

8. FlowTracker.track(SELECTED) — "🎯 Selected: fire-familiar · Target: dragon"

9. SummonerService.invokeFire("http://localhost:8091/execute", "dragon")
       │
       ▼ @CooldownProtected(familiarName = "fire-familiar", cooldownSeconds = 60)
       │
       ▼ CooldownAspect.enforceCooldown()
         ├── No previous call (or cooldown expired) → PROCEED
         └── Record lastCall = now

10. FlowTracker.track(INVOKE) — "📡 Calling familiar endpoint..."

11. RestClient.post("http://localhost:8091/execute")
        .body({target: "dragon"})
        .retrieve().body(Map.class)

12. [Fire Familiar @ 8091]
    FireFamiliarService.execute("dragon")
        Step 1 — SpellSearchTool.scoutSpell("dragon")
                 SpellRepository.findMostPowerfulByElement("dragon")
                 → No "dragon" element in DB → "No dragon spell found in the Librarium."
        Step 2 — AmplifyTool.amplify("No dragon spell found...")
                 → "Cannot amplify: No dragon spell found in the Librarium."

13. Summoner receives: "Cannot amplify: No dragon spell found..."

14. FlowTracker.track(RESULT) — "✅ Familiar responded successfully."

15. WebController builds model:
    - result = "Cannot amplify: No dragon spell found..."
    - flowEvents = [COMMAND, ROUTING x2, SELECTED, INVOKE, RESULT]
    - familiars = [fire(cooldown=58s), water(ready), earth(ready)]

16. Thymeleaf renders fragments/summon-response :: result
    HTMX processes:
    - Replaces #result-area with result box
    - OOB-swaps #flow-panel with step list
    - OOB-swaps #familiars-panel with updated cooldown bars
```

> **Tip:** Try `"Attack the dragon with fire"` → then immediately `"Use fire again"`.  
> The second request will be blocked by the AOP cooldown with the remaining wait time displayed.

---

## Testing

Run all tests from the parent directory:

```bash
cd spell-caster
mvn test
```

| Test class | Module | What it tests |
|------------|--------|---------------|
| `CooldownAspectTest` | summoner | AOP cooldown enforcement with injectable `MutableClock` |
| `FireFamiliarServiceTest` | familiar-fire | Sequential Scout → Amplify pipeline |
| `WaterFamiliarServiceTest` | familiar-water | Parallel StructuredTaskScope execution |
| `EarthFamiliarServiceTest` | familiar-earth | Iterative charge loop reaching/not-reaching threshold |

The `CooldownAspect` test is especially worth reading — it shows how dependency-injecting a `Clock` bean eliminates the need for `Thread.sleep` in time-based tests:

```java
@Test
void secondInvocationWithinCooldown_blocked() {
    testService.callFamiliar();
    mutableClock.setInstant(Instant.EPOCH.plusSeconds(30)); // fast-forward 30s
    String result = testService.callFamiliar();
    assertThat(result).contains("cooldown");
}
```

---

## Design decisions

### Why not use A2A?

The A2A protocol (supported in this repo's `stock-winner` module via `spring-ai-a2a-server-autoconfigure:0.2.0`) adds async task state machines, well-known endpoints, and a JSON-RPC wire format. For this demo the overhead is unnecessary — we want to show the _multi-agent routing and governance_ pattern, not the wire protocol. The custom `GET /agent-card` + `POST /execute` pair is easier to read and debug with nothing but `curl`.

### Why StructuredTaskScope instead of CompletableFuture?

`StructuredTaskScope` (JEP 453 / JEP 480, preview in Java 25) provides automatic:
- **Lifetime management** — child tasks cannot outlive the scope block.
- **Error propagation** — `allSuccessfulOrThrow()` cancels remaining tasks and re-throws on any failure.
- **Thread observability** — tasks run on named virtual threads, visible in thread dumps.

`CompletableFuture.allOf()` achieves similar results but leaks tasks on exceptions and requires manual cancellation.

### Why AOP for cooldowns, not a filter or interceptor?

A servlet filter or `HandlerInterceptor` would require the cooldown logic to know about HTTP paths, which breaks if the routing changes. A Spring `MethodInterceptor` on the service layer is HTTP-agnostic — it fires for the same method regardless of whether it is called from a REST controller, the Web UI controller, or a test.

### Why an injectable Clock?

`CooldownAspect` stores the last invocation time as an `Instant`. Using `Instant.now()` directly would require `Thread.sleep` in tests to advance time. An injectable `Clock` bean lets tests inject a `MutableClock` and call `setInstant()` to simulate any point in time — making tests fast and deterministic.

### Why ThreadLocal for session context in SummonerService?

`callFamiliar()` is a private method called after the AOP proxy boundary. Passing `sessionId` through the private call chain would require changing the signatures of `invokeFire`, `invokeWater`, and `invokeEarth` — but those methods are intercepted by `CooldownAspect`, meaning Spring proxies them, and changing their signatures would affect the proxy. Using `ThreadLocal` allows the `FlowTracker` to receive events from `callFamiliar()` without changing any method signatures. The `ThreadLocal` is always cleared in a `finally` block in `SummonerService.summon()`, preventing leaks. A future migration to Java 25's `ScopedValue` (JEP 481, final) is noted in the code.
