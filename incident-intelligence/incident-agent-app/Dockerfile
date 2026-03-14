# ──────────────────────────────────────────────────────────────────────────────
# Dockerfile for incident-agent-app
# ──────────────────────────────────────────────────────────────────────────────

# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy POMs for dependency resolution caching
COPY pom.xml                                ./pom.xml
COPY incident-mcp-server/pom.xml            ./incident-mcp-server/pom.xml
COPY incident-agent-app/pom.xml             ./incident-agent-app/pom.xml

RUN mvn dependency:go-offline -pl incident-agent-app -am --no-transfer-progress

# Copy source and build (skipping tests — tests run in CI without DB/AI services)
COPY incident-agent-app/src ./incident-agent-app/src

RUN mvn package -pl incident-agent-app -am -DskipTests --no-transfer-progress

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app

COPY --from=builder /build/incident-agent-app/target/incident-agent-app-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
