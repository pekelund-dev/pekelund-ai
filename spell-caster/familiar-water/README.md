# 🌊 Familiar Water — Parallel Agent

> The Water Familiar is the **parallel** member of the Spell Caster multi-agent system.  
> It demonstrates a **fan-out / fan-in** pattern using Java 25's `StructuredTaskScope`.

---

## Role in the system

The Summoner routes commands that require water-based attacks to this familiar.  
It forks two tool calls simultaneously and merges the results.

```
POST /execute  {target: "troll"}
       │
       ▼
WaterFamiliarService.execute("troll")
       │
       ├─── fan-out ─────────────────────────────────────────────┐
       │                                                          │
       │  CryoShatterTool.cryoShatter("troll")    MoonlightCascadeTool.moonlightCascade("troll")
       │  [virtual thread — Nexus channel]         [virtual thread — Forge channel]
       │                                                          │
       └──────────────── fan-in ─────────────────────────────────┘
                         │
                         ▼  scope.join() — blocks until BOTH complete
                         │
                         ▼
                    powerMerger(nexusResult, forgeResult)
                         │
                         ▼
                       Result
```

---

## What is StructuredTaskScope?

`StructuredTaskScope` (JEP 453, preview in Java 21–25) provides structured concurrency:

```java
try (var scope = StructuredTaskScope.open(
         StructuredTaskScope.Joiner.<String>allSuccessfulOrThrow())) {

    var nexusTask = scope.fork(() -> cryoShatterTool.cryoShatter(target));
    var forgeTask = scope.fork(() -> moonlightCascadeTool.moonlightCascade(target));

    scope.join();  // blocks here until both tasks finish

    return powerMerger(nexusTask.get(), forgeTask.get());
}
```

The `allSuccessfulOrThrow()` joiner:
- **Waits** for all forked tasks to complete.
- **Cancels** any remaining tasks if one throws.
- **Re-throws** the first exception encountered.
- **Guarantees** tasks cannot outlive the `try` block (structured lifetime).

Compared with `CompletableFuture.allOf()`, this approach has automatic lifetime management and cleaner cancellation semantics.

> **Build note:** `StructuredTaskScope` is a preview API in Java 25.  
> The Maven compiler plugin is configured with `--enable-preview` so no manual flags are needed.

---

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/agent-card` | Returns the familiar's capabilities for Summoner discovery |
| `POST` | `/execute` | Runs the parallel CryoShatter ∥ MoonlightCascade pipeline |

### `GET /agent-card`

```json
{
  "name": "water-familiar",
  "displayName": "Aqua — Water Familiar",
  "description": "Parallel water agent that simultaneously channels CryoShatter and MoonlightCascade using StructuredTaskScope, then power-merges both results.",
  "endpoint": "http://localhost:8092/execute",
  "skills": [
    {
      "name": "water_attack",
      "description": "Fan-out: cryo_shatter (Nexus) + moonlight_cascade (Forge) run in parallel. Fan-in: power merger combines both channel results."
    }
  ]
}
```

### `POST /execute`

**Request:**
```json
{ "target": "troll" }
```

**Response:**
```json
{
  "familiar": "water",
  "pattern": "parallel",
  "result": "💧 POWER MERGER — Dual-Channel Water Strike:\n┌─ Nexus Channel: ❄️ CRYO_SHATTER [Nexus Channel]: Target 'troll' crystallized and shattered with 87 ice power. Nexus energy threads: virtual-8\n└─ Forge Channel: 🌊 MOONLIGHT_CASCADE [Forge Channel]: Target 'troll' engulfed in lunar water cascade with 92 moon power. Forge energy threads: virtual-9\n⚡ Combined Water Power unleashed simultaneously via StructuredTaskScope!"
}
```

Note the thread names in the result (`virtual-8`, `virtual-9`) — each tool runs on its own virtual thread.

---

## Tools

### `CryoShatterTool` (Nexus channel)

```java
public String cryoShatter(String target)
// Power: 70–94 (randomized)
// Reports thread name to show parallel execution
```

### `MoonlightCascadeTool` (Forge channel)

```java
public String moonlightCascade(String target)
// Power: 80–99 (randomized)
// Reports thread name to show parallel execution
```

---

## Configuration

`src/main/resources/application.yml`:

```yaml
spring:
  threads:
    virtual:
      enabled: true   # Tomcat uses virtual threads; StructuredTaskScope forks virtual threads too

server:
  port: 8092
```

---

## Running

```bash
cd familiar-water
mvn spring-boot:run
```

Verify:

```bash
curl http://localhost:8092/agent-card
curl -X POST http://localhost:8092/execute \
     -H "Content-Type: application/json" \
     -d '{"target": "troll"}'
```

---

## Tests

```bash
mvn test -pl familiar-water
```

`WaterFamiliarServiceTest` verifies:
1. Both channels complete and the result contains the power-merger header.
2. The result contains output from both `CryoShatter` and `MoonlightCascade`.
