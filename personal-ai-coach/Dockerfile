# ──────────────────────────────────────────────────────────────────────────────
# Dockerfile for Personal AI Coach
# Multi-stage build: Maven build → Runtime image
# ──────────────────────────────────────────────────────────────────────────────

# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-25 AS builder

WORKDIR /build

# Copy POM for dependency resolution caching
COPY pom.xml ./

RUN mvn dependency:go-offline --no-transfer-progress

# Copy source and build
COPY src ./src

RUN mvn package -DskipTests --no-transfer-progress

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre

RUN groupadd -r appgroup && useradd -r -g appgroup appuser
USER appuser

WORKDIR /app

COPY --from=builder /build/target/personal-ai-coach-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
