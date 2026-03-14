# ── Stage 1: Build ────────────────────────────────────────────────────────────
# Build the entire multi-module Maven project and extract the agent-app JAR.
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

# Copy parent + module POMs first to cache the dependency download layer
COPY pom.xml .
COPY stock-mcp-server/pom.xml stock-mcp-server/pom.xml
COPY stock-agent-app/pom.xml  stock-agent-app/pom.xml

# Pre-fetch dependencies for the modules we will build (cache-friendly)
RUN mvn dependency:go-offline -pl stock-agent-app -am -q --no-transfer-progress 2>/dev/null || true

# Copy sources and build
COPY stock-mcp-server/src stock-mcp-server/src
COPY stock-agent-app/src  stock-agent-app/src

RUN mvn -pl stock-agent-app -am clean package -DskipTests -q --no-transfer-progress

# ── Stage 2: Run ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/stock-agent-app/target/stock-agent-app-*.jar app.jar

EXPOSE 8082

# STOCK_MCP_SERVER_URL is set by docker-compose to point at the mcp-server container.
# GEMINI_API_KEY must be provided at runtime (via docker-compose .env or -e flag).
ENTRYPOINT ["java", "-jar", "app.jar"]
