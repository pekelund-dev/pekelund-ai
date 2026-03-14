# ──────────────────────────────────────────────────────────────────────────────
# Dockerfile for incident-mcp-server
# ──────────────────────────────────────────────────────────────────────────────
# Uses a two-stage build:
#   1. Build stage   – compiles the Maven project inside a JDK container
#   2. Runtime stage – copies only the fat-JAR into a lean JRE image
#
# This keeps the final image small and free of build-time tooling.
# ──────────────────────────────────────────────────────────────────────────────

# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy parent POM first so Maven can resolve the BOM
COPY pom.xml                                ./pom.xml
COPY incident-mcp-server/pom.xml            ./incident-mcp-server/pom.xml
COPY incident-agent-app/pom.xml             ./incident-agent-app/pom.xml

# Pre-fetch all dependencies (this layer is cached unless POM files change)
RUN mvn dependency:go-offline -pl incident-mcp-server -am --no-transfer-progress

# Copy source and build
COPY incident-mcp-server/src ./incident-mcp-server/src

RUN mvn package -pl incident-mcp-server -am -DskipTests --no-transfer-progress

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

# Security: run as non-root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app

COPY --from=builder /build/incident-mcp-server/target/incident-mcp-server-*.jar app.jar

# Expose the MCP server HTTP port
EXPOSE 8081

# JVM flags:
#   -XX:+UseContainerSupport    — respects Docker CPU/memory limits
#   -XX:MaxRAMPercentage=75.0   — leave headroom for OS
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
