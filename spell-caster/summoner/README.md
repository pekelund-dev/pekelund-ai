# 🔮 The Summoner — Orchestrator Agent

> The Summoner is the **central orchestrator** of the Spell Caster multi-agent system.  
> It receives natural-language commands, uses a Gemini LLM to decide which familiar to invoke,
> enforces governance rules via AOP, maintains short-term conversation memory, and serves a
> browser-based Web UI.

---

## Table of Contents

1. [What the Summoner does](#what-the-summoner-does)
2. [Service discovery](#service-discovery)
3. [LLM routing](#llm-routing)
4. [Cooldown governance (AOP)](#cooldown-governance-aop)
5. [Short-term memory](#short-term-memory)
6. [Flow tracking](#flow-tracking)
7. [Web UI](#web-ui)
8. [REST API](#rest-api)
9. [Configuration](#configuration)
10. [Running](#running)
11. [Tests](#tests)
12. [Component reference](#component-reference)

---

## What the Summoner does

```
User command: "Attack the dragon with fire!"
        │
        ▼
[1] AgentDiscoveryService — provides list of familiars discovered at startup
[2] Gemini LLM (ChatClient) — decides which familiar and target
        │
        │  LLM response: "fire-familiar|dragon"
        │
        ▼
[3] CooldownAspect (AOP) — checks if fire-familiar is on cooldown
        │
        │  Passed → record invocation time
        │
        ▼
[4] RestClient → POST http://localhost:8091/execute  {target: "dragon"}
        │
        │  Familiar responds
        │
        ▼
[5] FlowTracker — collects all steps for the Web UI
[6] Return result to caller (REST response or HTMX fragment)
```

---

## Service discovery

At startup (`@PostConstruct`), `AgentDiscoveryService` calls `GET /agent-card` on each configured
familiar URL and stores the returned `AgentCard` records:

```
startup
  ├── GET http://localhost:8091/agent-card → AgentCard(name="fire-familiar", ...)
  ├── GET http://localhost:8092/agent-card → AgentCard(name="water-familiar", ...)
  └── GET http://localhost:8093/agent-card → AgentCard(name="earth-familiar", ...)
```

If a familiar is unreachable at startup, a warning is logged and it is simply omitted from the routing
table — the Summoner continues with the familiars that are online. When all three are online the log looks like:

```
🔮 Summoner initiating familiar discovery...
✅ Discovered familiar: Ignis — Fire Familiar at http://localhost:8091/execute with skills: [fire_attack]
✅ Discovered familiar: Aqua — Water Familiar at http://localhost:8092/execute with skills: [water_attack]
✅ Discovered familiar: Terra — Earth Familiar at http://localhost:8093/execute with skills: [earth_attack]
🔮 Discovery complete. 3 familiar(s) available.
```

The familiar URLs can be overridden with environment variables for containerized deployments:

```
FAMILIAR_FIRE_URL=http://fire-service:8091
FAMILIAR_WATER_URL=http://water-service:8092
FAMILIAR_EARTH_URL=http://earth-service:8093
```

---

## LLM routing

The Summoner builds a Gemini prompt containing:
1. A **system prompt** listing all discovered familiars and their descriptions.
2. The **user's command** as the user message.
3. A **memory advisor** that injects the last 20 messages from the session into the prompt context.

```java
String routing = chatClient.prompt()
    .system(systemPrompt)          // includes familiar descriptions + rules
    .user(command)                 // user's natural-language command
    .advisors(advisor ->
        advisor.param("chat_memory_conversation_id", sessionId))
    .call()
    .content();
// Expected output: "fire-familiar|dragon"
```

The LLM is instructed to respond with **only** `<familiar-name>|<target>`. This deterministic output format is then parsed with a simple `split("|")`.

**Model:** `gemini-2.0-flash` at temperature `0.2` (low temperature for consistent routing decisions).

---

## Cooldown governance (AOP)

### Annotation

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CooldownProtected {
    String familiarName();
    long cooldownSeconds() default 60;
}
```

### Aspect

`CooldownAspect` intercepts every `@CooldownProtected` method. If the same `familiarName` was invoked within the last `cooldownSeconds`, the aspect returns a blocked message **immediately** — the actual REST call to the familiar is never made:

```java
@Around("@annotation(cooldownProtected)")
public Object enforceCooldown(ProceedingJoinPoint joinPoint,
                              CooldownProtected cooldownProtected) throws Throwable {
    String familiarName = cooldownProtected.familiarName();
    Instant lastCall = lastInvocationTimes.get(familiarName);
    Instant now = clock.instant();

    if (lastCall != null) {
        long elapsed = now.getEpochSecond() - lastCall.getEpochSecond();
        if (elapsed < cooldownProtected.cooldownSeconds()) {
            long remaining = cooldownProtected.cooldownSeconds() - elapsed;
            return "⛔ Familiar '" + familiarName + "' is on cooldown. Please wait "
                   + remaining + " more second(s).";
        }
    }

    lastInvocationTimes.put(familiarName, now);  // update last-call time
    return joinPoint.proceed();                   // call the familiar
}
```

The three guarded methods in `SummonerService`:

```java
@CooldownProtected(familiarName = "fire-familiar")
public String invokeFire(String endpoint, String target) { ... }

@CooldownProtected(familiarName = "water-familiar")
public String invokeWater(String endpoint, String target) { ... }

@CooldownProtected(familiarName = "earth-familiar")
public String invokeEarth(String endpoint, String target) { ... }
```

### Monitoring cooldowns via the Web UI

The sidebar in the Web UI shows a live countdown bar for each familiar that is on cooldown,
updated every 5 seconds by HTMX polling and immediately after each summon via OOB swap.
The bar width is calculated from `CooldownAspect.getRemainingCooldowns()` and the registered
cooldown window (`getRegisteredCooldownSeconds()`).

### Testing cooldowns without Thread.sleep

`CooldownAspect` accepts a `java.time.Clock` bean:

```java
// Production (SummonerConfig)
@Bean
public Clock clock() { return Clock.systemUTC(); }
```

In tests a `MutableClock` can fast-forward time:

```java
testService.callFamiliar();                              // first call OK
mutableClock.setInstant(Instant.EPOCH.plusSeconds(30)); // advance 30 s
String result = testService.callFamiliar();              // still on 60s cooldown
assertThat(result).contains("cooldown");
```

---

## Short-term memory

`MessageWindowChatMemory` stores up to 20 messages per session in memory:

```java
ChatMemory chatMemory = MessageWindowChatMemory.builder()
    .chatMemoryRepository(new InMemoryChatMemoryRepository())
    .maxMessages(20)
    .build();
```

The `MessageChatMemoryAdvisor` is set as a **default advisor** on the `ChatClient`. Every LLM call automatically reads previous messages for the session (via `chat_memory_conversation_id`) and appends the new exchange.

This gives the LLM context about what was summoned previously, enabling the "don't summon the same familiar twice in a row" rule — encoded entirely in the system prompt, not in imperative Java code.

**Memory is in-process and not persisted.** Restarting the Summoner clears all sessions. The Web UI persists the session UUID in `localStorage` so the browser sends the same ID across page reloads, but the memory on the server side is gone.

---

## Flow tracking

`FlowTracker` captures each step of the execution pipeline as a `FlowEvent`:

| Type | Emoji | When emitted |
|------|-------|-------------|
| `COMMAND` | 📨 | Command received |
| `ROUTING` | 🧠 | LLM query started |
| `ROUTING` | 🔀 | LLM response parsed |
| `SELECTED` | 🎯 | Familiar and target identified |
| `INVOKE` | 📡 | REST call to familiar started |
| `COOLDOWN` | ⛔ | AOP blocked the call |
| `RESULT` | ✅ | Familiar responded successfully |
| `ERROR` | ⚠️ | No familiars discovered or routing parse failed |

Events are stored in a `ConcurrentHashMap<sessionId, List<FlowEvent>>` and cleared when the Web UI
fetches them (`getAndClear(sessionId)`), so they are shown exactly once per request.

`SummonerService` uses a `ThreadLocal<String>` to carry the `sessionId` into `callFamiliar()` without changing the method signatures that are intercepted by AOP. The `ThreadLocal` is always cleaned up in a `finally` block.

---

## Web UI

The Summoner serves a Thymeleaf + HTMX dashboard at `http://localhost:8090/`.

### Thymeleaf controllers

| Controller | Path | Returns |
|------------|------|---------|
| `WebController` | `GET /` | `templates/index.html` (full page) |
| `WebController` | `GET /ui/familiars` | `fragments/familiars :: cards` |
| `WebController` | `POST /ui/summon` | `fragments/summon-response :: result` |

### HTMX architecture

```
index.html
├── #familiars-panel
│     hx-get="/ui/familiars"
│     hx-trigger="load, every 5s"   ← polls familiar status + cooldowns
│     hx-swap="innerHTML"
│
├── <form>
│     hx-post="/ui/summon"
│     hx-target="#result-area"
│     hx-indicator="#spinner"
│     hx-disabled-elt="find button"
│
├── #flow-panel                      ← updated via OOB swap in summon response
└── #result-area                     ← updated via hx-target in summon response
```

The `POST /ui/summon` response contains:
1. The main result fragment → replaces `#result-area`.
2. `hx-swap-oob="true"` on `#flow-panel` → replaces execution flow panel.
3. `hx-swap-oob="true"` on `#familiars-panel` → updates cooldown bars immediately.

### Session persistence

The JavaScript on `index.html` reads/writes `localStorage`:

```javascript
let sid = localStorage.getItem('spellcaster_session');
if (!sid) { sid = crypto.randomUUID(); localStorage.setItem('spellcaster_session', sid); }
document.getElementById('sessionId').value = sid;
```

This means the LLM's memory of previous summons (the "don't repeat" rule) survives browser refreshes.

---

## REST API

### `POST /summon`

```json
// Request
{ "command": "Attack the dragon with fire!", "sessionId": "optional-uuid" }

// Response
{
  "command": "Attack the dragon with fire!",
  "sessionId": "abc123",
  "result": "🔥 AMPLIFIED [x1.5]: Scout found: Inferno Blast (power=95)..."
}
```

If `sessionId` is omitted, the Summoner generates a random one.

### `GET /summon/familiars`

Returns the list of discovered agent cards:

```json
[
  {
    "name": "fire-familiar",
    "displayName": "Ignis — Fire Familiar",
    "description": "...",
    "endpoint": "http://localhost:8091/execute",
    "skills": [{ "name": "fire_attack", "description": "..." }]
  },
  ...
]
```

---

## Configuration

`src/main/resources/application.yml`:

```yaml
spring:
  ai:
    google:
      genai:
        api-key: ${GEMINI_API_KEY}
        chat.options:
          model: gemini-2.0-flash
          temperature: 0.2      # Low temperature for deterministic routing
  threads:
    virtual:
      enabled: true             # Virtual threads for all Tomcat request threads

server:
  port: 8090

summoner:
  familiars:
    fire-url:  ${FAMILIAR_FIRE_URL:http://localhost:8091}
    water-url: ${FAMILIAR_WATER_URL:http://localhost:8092}
    earth-url: ${FAMILIAR_EARTH_URL:http://localhost:8093}
```

---

## Running

```bash
export GEMINI_API_KEY=your-key-here
cd summoner
mvn spring-boot:run
```

Open **http://localhost:8090/** in a browser.

For full functionality all three familiars must be running first.  
The Summoner starts even if no familiars are online, but all summon attempts will return an error until at least one familiar is discovered.

---

## Tests

```bash
mvn test -pl summoner
```

`CooldownAspectTest` tests the governance layer in isolation using a minimal Spring context
(`@SpringBootTest(classes = {TestConfig.class, TestService.class, CooldownAspect.class})`).
No Gemini key required.

---

## Component reference

| Class | Package | Role |
|-------|---------|------|
| `SummonerApplication` | `.summoner` | Spring Boot entry point |
| `SummonerConfig` | `.config` | Beans: `Clock`, `ChatMemory`, `ChatClient`, `RestClient` |
| `SummonerService` | `.service` | Main orchestration logic, LLM call, routing |
| `AgentDiscoveryService` | `.service` | `@PostConstruct` familiar discovery via Agent Cards |
| `FlowTracker` | `.service` | Per-session execution event store |
| `CooldownAspect` | `.aspect` | AOP: enforces 60-second cooldown per familiar |
| `CooldownProtected` | `.annotation` | Annotation that marks methods for cooldown governance |
| `WebController` | `.controller` | Thymeleaf + HTMX Web UI endpoints |
| `SummonerController` | `.controller` | JSON REST API (`/summon`) |
| `AgentCard` | `.model` | Familiar capability descriptor (record) |
| `AgentSkill` | `.model` | Individual skill within an agent card (record) |
| `FamiliarStatus` | `.model` | View-model for Web UI sidebar (record) |
| `FlowEvent` | `.model` | Single execution step captured by FlowTracker (record) |
| `ExecuteRequest` | `.model` | Request body for `POST /execute` on familiars (record) |
| `ExecuteResponse` | `.model` | Response body from familiars (record) |
