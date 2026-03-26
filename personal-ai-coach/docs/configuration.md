# Konfiguration — Personal AI Coach

## Miljövariabler

### Obligatoriska

| Variabel | Beskrivning | Exempel |
|----------|-------------|---------|
| `GEMINI_API_KEY` | Google Gemini API-nyckel | `AIza...` |
| `GOOGLE_CLIENT_ID` | Google OAuth2 klient-ID | `123456.apps.googleusercontent.com` |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 klient-hemlighet | `GOCSPX-...` |

### Valfria (med standardvärden)

| Variabel | Standard | Beskrivning |
|----------|----------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/coach_db` | Databas-URL |
| `DB_USERNAME` | `coach` | Databas-användare |
| `DB_PASSWORD` | `coach_secret` | Databas-lösenord |
| `GEMINI_ROUTER_MODEL` | `gemini-2.0-flash-lite` | Modell för klassificering |
| `GEMINI_SIMPLE_MODEL` | `gemini-2.0-flash` | Modell för enkla uppgifter |
| `GEMINI_COMPLEX_MODEL` | `gemini-2.5-pro` | Modell för komplexa uppgifter |
| `GEMINI_SIMPLE_MAX_TOKENS` | `1024` | Max tokens för enkel modell |
| `GEMINI_COMPLEX_MAX_TOKENS` | `4096` | Max tokens för komplex modell |

## Google OAuth2-konfiguration

### Steg 1: Skapa projekt i Google Cloud Console

1. Gå till [Google Cloud Console](https://console.cloud.google.com/)
2. Skapa ett nytt projekt eller välj ett befintligt
3. Aktivera "Google+ API" eller "People API"

### Steg 2: Konfigurera OAuth2-samtyckesskärm

1. Gå till **APIs & Services → OAuth consent screen**
2. Välj **External** user type
3. Fyll i applikationsnamn: "Personal AI Coach"
4. Lägg till scope: `email`, `profile`, `openid`

### Steg 3: Skapa OAuth2-klient

1. Gå till **APIs & Services → Credentials**
2. Klicka **Create Credentials → OAuth 2.0 Client ID**
3. Välj **Web application**
4. Lägg till Authorized redirect URI: `http://localhost:8080/login/oauth2/code/google`
5. Kopiera klient-ID och klient-hemlighet

### Steg 4: Konfigurera miljövariabler

```bash
export GOOGLE_CLIENT_ID=din-klient-id.apps.googleusercontent.com
export GOOGLE_CLIENT_SECRET=din-klient-hemlighet
```

## Gemini API-nyckel

1. Gå till [Google AI Studio](https://ai.google.dev/)
2. Skapa en API-nyckel (gratis)
3. Konfigurera:

```bash
export GEMINI_API_KEY=din-api-nyckel
```

## Modellkonfiguration

### Klassificeringsmodell (Router)

Klassificerar varje inkommande fråga som SIMPLE eller COMPLEX.

```yaml
coach:
  agent:
    router-model: gemini-2.0-flash-lite  # Billigast, 5 tokens max
```

### Enkel modell (Simple)

Hanterar enkla uppslag: väder, tid, läsa e-post.

```yaml
coach:
  agent:
    simple-model: gemini-2.0-flash
    simple-max-tokens: 1024
```

### Komplex modell (Complex)

Hanterar komplexa uppgifter: planering, analys, resonemang.

```yaml
coach:
  agent:
    complex-model: gemini-2.5-pro
    complex-max-tokens: 4096
```

## Databashantering

### PostgreSQL med pgvector

Docker Compose startar automatiskt PostgreSQL med pgvector-extension.
Flyway hanterar databasmigrationer automatiskt vid start.

### Manuell databassetup

Om du vill köra PostgreSQL utan Docker:

```sql
CREATE DATABASE coach_db;
CREATE USER coach WITH PASSWORD 'coach_secret';
GRANT ALL PRIVILEGES ON DATABASE coach_db TO coach;

-- pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;
```

## Användarpreferenser

Preferenser lagras per användare och kategori i tabellen `user_preferences`.

### Tillgängliga kategorier

| Kategori | Exempel-nycklar |
|----------|-----------------|
| `calendar` | `default_calendar`, `reminder_minutes` |
| `email` | `primary_account`, `vip_senders` |
| `finance` | `currency`, `stock_portfolio_ids` |
| `menu` | `dietary_restrictions`, `servings_default` |
| `vacation` | `preferred_airlines`, `home_airport` |
| `coaching` | `goals`, `check_in_frequency` |
| `general` | `language`, `theme`, `notification_prefs` |
