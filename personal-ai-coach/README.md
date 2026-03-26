# Personal AI Coach 🎯

**Din personliga AI-coach — en svensk AI-baserad assistent för kalender, e-post, ekonomi, menyplanering, semester, todo-listor och personlig coaching.**

## Översikt

Personal AI Coach är en AI-driven personlig assistent byggd med Java 25, Spring Boot 4 och Spring AI 2.0. Applikationen använder Google Gemini som LLM med dynamisk modellval — snabb modell för enkla frågor, kraftfull modell för komplexa uppgifter.

### Funktioner

| Agent | Beskrivning |
|-------|-------------|
| 📅 **Kalender** | Översikt och hantering av kalenderhändelser |
| 📧 **E-post** | Översikt, filtrering och markering av viktiga e-postmeddelanden |
| 💰 **Ekonomi** | Aktier, sparande, utgifter och ekonomisk planering |
| 🍽️ **Menyplanering** | Veckomenyförslag med automatisk inköpslista |
| ✈️ **Semester** | Reseplanering med flyg, tåg och hotellsökning |
| ✅ **Att göra** | Uppgiftshantering med automatisk skapande från andra agenter |
| 🎯 **Coaching** | Personlig coaching, målsättning och motivation |

### Teknisk stack

- **Java 25** med moderna features
- **Spring Boot 4.0.3** — senaste versionen
- **Spring AI 2.0.0-M2** — med Gemini-integration
- **PostgreSQL + pgvector** — SQL och vektordatabas
- **Google OAuth2** — inloggning med Google
- **Thymeleaf + HTMX** — modern UI utan SPA-ramverk
- **Docker Compose** — enkel lokal körning

## Snabbstart

### Förutsättningar

- Docker & Docker Compose
- Google Cloud Console-konto med OAuth2-klient konfigurerat
- Gemini API-nyckel (gratis på [Google AI Studio](https://ai.google.dev/))

### Kör med Docker

```bash
# Ställ in miljövariabler
export GEMINI_API_KEY=din-api-nyckel
export GOOGLE_CLIENT_ID=din-klient-id
export GOOGLE_CLIENT_SECRET=din-klient-hemlighet

# Starta
cd personal-ai-coach
docker compose up --build
```

Öppna [http://localhost:8080](http://localhost:8080) i din webbläsare.

### Kör lokalt (utan Docker)

Kräver Java 25, Maven 3.9+ och en PostgreSQL-instans med pgvector.

```bash
cd personal-ai-coach

# Konfigurera miljövariabler
export GEMINI_API_KEY=din-api-nyckel
export GOOGLE_CLIENT_ID=din-klient-id
export GOOGLE_CLIENT_SECRET=din-klient-hemlighet
export DB_URL=jdbc:postgresql://localhost:5432/coach_db
export DB_USERNAME=coach
export DB_PASSWORD=coach_secret

# Bygg och kör
mvn spring-boot:run
```

### Kör tester

```bash
cd personal-ai-coach
mvn test
```

## Arkitektur

Se [docs/architecture.md](docs/architecture.md) för detaljerad arkitekturdokumentation.

### Översikt

```
┌──────────────────────────────────────────────────┐
│                 Thymeleaf + HTMX UI              │
│  ┌──────────┐ ┌──────────┐ ┌──────────────────┐ │
│  │Dashboard │ │ Chatt    │ │ Login (OAuth2)   │ │
│  └──────────┘ └──────────┘ └──────────────────┘ │
├──────────────────────────────────────────────────┤
│              Spring Boot Controllers             │
│  ┌──────────┐ ┌──────────┐ ┌──────────────────┐ │
│  │Dashboard │ │ Chat     │ │ API (REST)       │ │
│  └──────────┘ └──────────┘ └──────────────────┘ │
├──────────────────────────────────────────────────┤
│              Agent Service (Orchestrator)         │
│  ┌────────────────────────────────────────────┐  │
│  │ ModelRouter → Dynamic Model Selection      │  │
│  │ ┌─────────┐ ┌──────────┐ ┌──────────────┐ │  │
│  │ │ SIMPLE  │ │ COMPLEX  │ │ Classification│ │  │
│  │ │ Flash   │ │ Pro      │ │ Flash-Lite   │ │  │
│  │ └─────────┘ └──────────┘ └──────────────┘ │  │
│  └────────────────────────────────────────────┘  │
├──────────────────────────────────────────────────┤
│                     Agents                        │
│  📅 Kalender │ 📧 E-post │ 💰 Ekonomi │ 🍽️ Meny │
│  ✈️ Semester │ ✅ Todo   │ 🎯 Coaching          │
├──────────────────────────────────────────────────┤
│         PostgreSQL + pgvector (Docker)            │
│  ┌──────────────┐ ┌───────────────────────────┐  │
│  │ SQL Tables   │ │ Vector Store (Embeddings) │  │
│  └──────────────┘ └───────────────────────────┘  │
└──────────────────────────────────────────────────┘
```

### Dynamisk modellval

Systemet använder en **ModelRouter** som klassificerar varje inkommande fråga:

| Modell | Användning | Kostnad |
|--------|-----------|---------|
| `gemini-2.0-flash-lite` | Klassificering (SIMPLE/COMPLEX) | Lägst |
| `gemini-2.0-flash` | Enkla frågor (väder, tid, läsa e-post) | Låg |
| `gemini-2.5-pro` | Komplexa uppgifter (planering, analys) | Medel |

## Projektstruktur

```
personal-ai-coach/
├── pom.xml                          # Maven-konfiguration
├── Dockerfile                       # Multi-stage Docker-build
├── docker-compose.yml               # PostgreSQL + App
├── README.md                        # Denna fil
├── docs/
│   ├── architecture.md              # Detaljerad arkitektur
│   └── configuration.md             # Konfigurationsguide
└── src/
    ├── main/
    │   ├── java/dev/pekelund/coach/
    │   │   ├── PersonalAiCoachApplication.java
    │   │   ├── config/              # Säkerhet, AI, databas
    │   │   ├── agent/               # ModelRouter, AgentService
    │   │   │   ├── calendar/        # 📅 Kalenderagent
    │   │   │   ├── email/           # 📧 E-postagent
    │   │   │   ├── finance/         # 💰 Ekonomiagent
    │   │   │   ├── menu/            # 🍽️ Menyplaneringsagent
    │   │   │   ├── vacation/        # ✈️ Semesteragent
    │   │   │   ├── todo/            # ✅ Todo-agent
    │   │   │   └── coaching/        # 🎯 Coachingagent
    │   │   ├── controller/          # Dashboard, Chat, API
    │   │   ├── domain/              # JPA-entiteter
    │   │   ├── repository/          # Spring Data JPA
    │   │   └── service/             # Affärslogik
    │   └── resources/
    │       ├── application.yml      # Konfiguration
    │       ├── db/migration/        # Flyway-migrationer
    │       ├── templates/           # Thymeleaf-mallar
    │       └── static/css/          # Stilmallar
    └── test/                        # Enhetstester
```

## Konfiguration

Se [docs/configuration.md](docs/configuration.md) för alla konfigurationsmöjligheter.

## Licens

Privat projekt av pekelund.dev
