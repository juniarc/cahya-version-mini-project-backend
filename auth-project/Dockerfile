# syntax=docker/dockerfile:1

# ---------- Stage 1: build ----------
FROM maven:3.9-eclipse-temurin-24 AS build
WORKDIR /workspace

# Resolve dependencies first so this layer is cached between builds
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Copy sources and build the Spring Boot jar
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:24-jre-alpine AS runtime
WORKDIR /app

# Run as a non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /workspace/target/auth-project-0.0.1-SNAPSHOT.jar app.jar

# Matches server.port=8081 in application.properties
EXPOSE 8081

# Respect container memory limits; fail fast on OOM instead of limping along
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]