# ===================================================================
# LifeDrop Blood Bank - Multi-Stage Production Dockerfile
# Optimized for Java 21, Spring Boot 3, and Cloud Container Hosting (Render)
# ===================================================================

# -------------------------------------------------------------------
# Stage 1: Build Application
# -------------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Pre-fetch dependencies using pom.xml for Docker layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy application source code
COPY src ./src

# Package production JAR
RUN mvn clean package -DskipTests -B

# -------------------------------------------------------------------
# Stage 2: Lightweight Runtime Environment
# -------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create unprivileged system user for security
RUN addgroup -S lifedrop && adduser -S lifedrop -G lifedrop

# Copy built JAR from builder stage
COPY --from=builder /build/target/lifedrop-backend-1.0.0.jar /app/lifedrop-app.jar

# Assign ownership to unprivileged user
RUN chown -R lifedrop:lifedrop /app

USER lifedrop

# Default port
EXPOSE 8080

# Production JVM tuning for container environments
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

# Launch application with dynamic PORT injection and prod profile
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod} -Dserver.port=${PORT:-8080} -jar /app/lifedrop-app.jar"]

