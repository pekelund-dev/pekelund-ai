# Hitta Börsens Vinnare - Stock Winner AI System

AI-baserat system för att hitta börsens vinnare med Arne Kavastu Talvings strategier.

## Arkitektur

```
stock-winner/
├── stock-mcp-server/     # MCP-server med börsdata-verktyg (port 8083)
└── stock-agent-app/      # AI-agent med Gemini + A2A protokoll (port 8082)
```

### stock-mcp-server
MCP-server som exponerar verktyg för:
- **StockDataTools**: Hämta aktiekurser och historisk data från Yahoo Finance
- **MarketAnalysisTools**: Beräkna Kavastu momentum score, relativ styrka och trendanalys

### stock-agent-app
Spring AI-agent med:
- **Gemini LLM**: Google Gemini 2.0 Flash som LLM
- **Kavastu System Prompt**: Instruerar AI:n att följa Kavastus investeringsfilosofi
- **A2A Protocol**: Agent-to-Agent kommunikation för integration med andra agenter
- **REST API**: Direkt HTTP-åtkomst för enkel integration

## Kavastus Strategi

Arne Kavastu Talvings nyckelprinciper implementerade i systemet:

1. **Momentum** - Viktad 3/6/12-månaders avkastningsanalys
2. **Relativ styrka** - Jämförelse mot OMXSPI (Stockholmsbörsens index)
3. **Trendföljning** - MA50/MA200 trendanalys
4. **Marknadsfilter** - Bull/bear bedömning av OMXSPI
5. **Kontinuerlig ombalansering** - Sälj förlorare, öka i vinnare

## Teknisk Stack

- **Java 21** / **Spring Boot 4**
- **Spring AI 2.0.0-M2** med MCP och A2A stöd
- **Google Gemini 2.0 Flash** som LLM
- **Yahoo Finance API** för marknadsdata

## Kom Igång

### Förutsättningar
- Java 21+ (Spring Boot 4 kräver Java 21)
- Maven 3.9+
- Google Gemini API-nyckel

### Bygg projektet
```bash
cd stock-winner
mvn clean package -DskipTests
```

### Starta MCP-servern
```bash
cd stock-mcp-server
mvn spring-boot:run
# Startar på port 8083
```

### Starta Agent-appen
```bash
cd stock-agent-app
GEMINI_API_KEY=din-api-nyckel mvn spring-boot:run
# Startar på port 8082
```

## API-endpoints

### REST API (direkt HTTP)
```
GET  /a2a/api/stocks/analyze/{symbol}   - Analysera en aktie (t.ex. ABB.ST)
GET  /a2a/api/stocks/screen             - Screena efter börsens vinnare
GET  /a2a/api/stocks/market             - Marknadsanalys OMXSPI
POST /a2a/api/stocks/ask                - Fri fråga om aktier
```

### A2A Protocol (Agent-to-Agent)
```
GET  /a2a/card      - Agent card (metadata och capabilities)
POST /a2a           - Skicka meddelande till agenten (JSON-RPC 2.0)
GET  /a2a/tasks/{id} - Hämta task-status
```

## Exempel

```bash
# Analysera en aktie
curl http://localhost:8082/a2a/api/stocks/analyze/ABB.ST

# Hitta börsens vinnare
curl http://localhost:8082/a2a/api/stocks/screen

# Fråga om marknadsläget
curl http://localhost:8082/a2a/api/stocks/market

# Ställ en fri fråga
curl -X POST http://localhost:8082/a2a/api/stocks/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "Vilka aktier ska jag köpa enligt Kavastu just nu?"}'

# Via A2A protocol
curl -X POST http://localhost:8082/a2a \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "sendMessage",
    "id": "1",
    "params": {
      "message": {
        "role": "user",
        "parts": [{"type": "text", "text": "Analysera MYCRONIC med Kavastu strategi"}]
      }
    }
  }'
```
