# Arkitektur — Personal AI Coach

## Översikt

Personal AI Coach är en monolitisk Spring Boot-applikation med en agentbaserad arkitektur. Varje funktionsområde hanteras av en specialiserad agent som har sina egna verktyg (tools) tillgängliga via Spring AI:s verktygssystem.

## Systemarkitektur

### Lager

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                         │
│  Thymeleaf + HTMX (server-side rendering med partiella      │
│  uppdateringar via HTMX-fragment)                            │
├─────────────────────────────────────────────────────────────┤
│                    Controller Layer                           │
│  DashboardController — Huvudsida med agentöversikt           │
│  ChatController — HTMX-baserad chatt per agent               │
│  ApiController — REST API för programmatisk åtkomst          │
├─────────────────────────────────────────────────────────────┤
│                    Agent Layer                                │
│  AgentService — Orchestrerar modelval och chattflöde          │
│  ModelRouter — Klassificerar uppgifter som SIMPLE/COMPLEX     │
│  7 specialiserade agenter med @Tool-annoterade metoder        │
├─────────────────────────────────────────────────────────────┤
│                    Service Layer                              │
│  UserService — Användarhantering via OAuth2                   │
│  ConfigurationService — Användarpreferenser                   │
├─────────────────────────────────────────────────────────────┤
│                    Data Layer                                 │
│  Spring Data JPA — SQL-åtkomst                               │
│  PGVector — Vektorlagring för embeddings                     │
│  Flyway — Databasmigrationer                                 │
├─────────────────────────────────────────────────────────────┤
│                    Infrastructure                             │
│  PostgreSQL + pgvector — SQL + vektordatabas                 │
│  Google OAuth2 — Autentisering                               │
│  Google Gemini — LLM-tjänst                                  │
└─────────────────────────────────────────────────────────────┘
```

### Agent-arkitektur

Varje agent är en Spring `@Component` med `@Tool`-annoterade metoder. Spring AI:s `MethodToolCallbackProvider` registrerar alla agentmetoder som verktyg tillgängliga för ChatClient.

```java
@Component
public class CalendarAgent {
    @Tool(description = "Visa dagens kalenderhändelser")
    public String getTodaysEvents() { ... }
}
```

**Fördelar:**
- Agenter är oberoende och kan enkelt läggas till eller tas bort
- Varje agent kan ha egna beroenden (repositories, externa API:er)
- Verktyg registreras automatiskt via Spring AI
- Agenter kan återanvändas i andra applikationer

### Dynamisk modellval

```
Användarmeddelande
       │
       ▼
┌──────────────┐
│ ModelRouter   │  ← gemini-2.0-flash-lite (5 tokens, 0 temp)
│ SIMPLE/COMPLEX│
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ AgentService  │
│              │
│  SIMPLE  ───→ gemini-2.0-flash   (snabb, billig)
│  COMPLEX ───→ gemini-2.5-pro     (kraftfull reasoning)
│              │
└──────┬───────┘
       │
       ▼
    AI-svar + verktygsanrop
```

### Databas

**PostgreSQL** med **pgvector**-extension ger:
- **SQL-tabeller** för användare, preferenser, todos, menyer, inköpslistor, konversationshistorik
- **Vektorlagring** via pgvector för framtida semantisk sökning

**Databasschema (V1):**

| Tabell | Beskrivning |
|--------|-------------|
| `users` | Användarkonton (Google OAuth2 ID) |
| `user_preferences` | Nyckel-värde-inställningar per kategori |
| `todo_items` | Uppgifter med prioritet, status, kategori |
| `menu_plans` | Veckomenyförslag per dag |
| `shopping_list_items` | Inköpslista kopplad till menyer |
| `conversation_history` | Konversationslogg per agent |

### Säkerhet

- **Google OAuth2** via Spring Security
- Alla sidor kräver autentisering utom `/login` och statiska resurser
- Sessionsbaserad autentisering med CSRF-skydd
- Användardata isoleras per Google-ID

### Utbyggbarhet

**Lägga till en ny agent:**

1. Skapa en ny klass i `agent/nyagent/`
2. Annotera med `@Component`
3. Lägg till `@Tool`-metoder
4. Registrera i `AgentConfig.toolCallbackProvider()`
5. Lägg till agenttyp i `AgentType`-enum

**Lägga till ny konfiguration:**

1. Definiera nycklar i `UserPreference` med kategori
2. Använd `ConfigurationService` för att läsa/skriva

## Driftsmiljö

### Lokal utveckling
- Docker Compose med PostgreSQL + pgvector
- Hot reload via Spring Boot DevTools
- H2-databas för tester

### GCP-driftsättning
- Cloud Run för applikationen
- Cloud SQL for PostgreSQL med pgvector
- Secret Manager för API-nycklar
- Cloud Build för CI/CD

## Framtida utökningar

- **MCP-protokoll**: Exponera agenter som MCP-servrar
- **A2A-protokoll**: Agent-till-agent-kommunikation
- **Riktiga integrationer**: Gmail API, Google Calendar API, börs-API
- **Vektorsökning**: Semantisk sökning i konversationshistorik
- **Streaming**: SSE-baserade svar för bättre UX
- **Notifikationer**: Push-notiser för viktiga händelser
