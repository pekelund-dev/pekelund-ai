# 🌍 Familiar Earth — Iterative Agent

> The Earth Familiar is the **iterative** member of the Spell Caster multi-agent system.  
> It demonstrates a **feedback loop** pattern: a tool is called repeatedly until a condition is met.

---

## Role in the system

The Summoner routes commands that require earth-based or seismic attacks to this familiar.  
It accumulates energy one charge at a time until a threshold is crossed.

```
POST /execute  {target: "golem"}
       │
       ▼
EarthFamiliarService.execute("golem")
       │
       ▼  totalEnergy = 0.0, iteration = 0
       │
       ├─▶ SeismicChargeTool.seismicCharge(1)  →  charge = 17.5
       │   totalEnergy = 17.5  [< 100.0, continue]
       │
       ├─▶ SeismicChargeTool.seismicCharge(2)  →  charge = 22.3
       │   totalEnergy = 39.8  [< 100.0, continue]
       │
       ├─▶ SeismicChargeTool.seismicCharge(3)  →  charge = 27.1
       │   totalEnergy = 66.9  [< 100.0, continue]
       │
       ├─▶ SeismicChargeTool.seismicCharge(4)  →  charge = 32.8
       │   totalEnergy = 99.7  [< 100.0, continue]
       │
       └─▶ SeismicChargeTool.seismicCharge(5)  →  charge = 35.0
           totalEnergy = 134.7  [≥ 100.0 → THRESHOLD REACHED, stop]
       │
       ▼
Result: "🌍 SEISMIC_CHARGE Iterative Pattern — Attack on 'golem': ... ✅ THRESHOLD REACHED"
```

---

## Loop parameters

| Parameter | Value | Description |
|-----------|-------|-------------|
| `ENERGY_THRESHOLD` | 100.0 | Loop continues while `totalEnergy < 100.0` |
| `MAX_ITERATIONS` | 10 | Safety cap to prevent infinite loops |

The loop exits when **either** condition is satisfied — whichever comes first. If max iterations is reached before the threshold, the result notes a "partial energy release" rather than throwing.

---

## SeismicCharge formula

```java
// BASE_CHARGE = 15.0, VARIANCE = 10.0
double charge = BASE_CHARGE + (iteration × 2.5) + (Math.random() × VARIANCE);
```

With each iteration the base charge grows (early iterations give ~17.5, later ones ~37.5), meaning threshold is typically reached in 3–6 iterations.

The result rounds to one decimal place (`Math.round(charge * 10.0) / 10.0`).

---

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/agent-card` | Returns the familiar's capabilities for Summoner discovery |
| `POST` | `/execute` | Runs the seismic charge loop |

### `GET /agent-card`

```json
{
  "name": "earth-familiar",
  "displayName": "Terra — Earth Familiar",
  "description": "Iterative earth agent that loops seismic_charge until energy threshold (100) is reached or max iterations (10) exceeded.",
  "endpoint": "http://localhost:8093/execute",
  "skills": [
    {
      "name": "earth_attack",
      "description": "Loop seismic_charge to accumulate seismic energy. Releases the attack when energy ≥ 100 or after 10 iterations."
    }
  ]
}
```

### `POST /execute`

**Request:**
```json
{ "target": "golem" }
```

**Response (threshold reached):**
```json
{
  "familiar": "earth",
  "pattern": "iterative",
  "result": "🌍 SEISMIC_CHARGE Iterative Pattern — Attack on 'golem':\n  Charge #1: +17.5 energy (total: 17.5)\n  Charge #2: +22.3 energy (total: 39.8)\n  Charge #3: +27.1 energy (total: 66.9)\n  Charge #4: +32.8 energy (total: 99.7)\n  Charge #5: +35.0 energy (total: 134.7)\n✅ THRESHOLD REACHED — Attack released!\nTotal energy: 134.7 | Iterations: 5 | Threshold: 100.0"
}
```

---

## Tools

### `SeismicChargeTool`

```java
// Performs one seismic charge, returning the energy generated.
// iteration: affects the base charge (grows with each call)
public double seismicCharge(int iteration)
```

This is a **general function tool** (deterministic calculation with a small random component). It has no external dependencies — no DB, no HTTP calls.

---

## Configuration

`src/main/resources/application.yml`:

```yaml
spring:
  threads:
    virtual:
      enabled: true   # All Tomcat request threads are virtual threads

server:
  port: 8093
```

---

## Running

```bash
cd familiar-earth
mvn spring-boot:run
```

Verify:

```bash
curl http://localhost:8093/agent-card
curl -X POST http://localhost:8093/execute \
     -H "Content-Type: application/json" \
     -d '{"target": "golem"}'
```

---

## Tests

```bash
mvn test -pl familiar-earth
```

`EarthFamiliarServiceTest` verifies:
1. A typical target reaches the threshold within `MAX_ITERATIONS` and reports "THRESHOLD REACHED".
2. The result contains the charge log with iteration details.
