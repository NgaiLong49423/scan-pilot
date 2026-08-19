# Stage 1: Build stage
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and package application
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal runtime stage
FROM eclipse-temurin:21-jre-alpine AS runner

# Security: create non-root user scanpilot (UID 10001)
RUN addgroup -g 10001 scanpilot && \
    adduser -u 10001 -G scanpilot -s /bin/sh -D scanpilot

WORKDIR /app

# Copy built artifact from builder stage and assign non-root ownership
COPY --from=builder /build/target/*.jar /app/app.jar
RUN chown -R scanpilot:scanpilot /app

# Switch to non-root user
USER scanpilot:scanpilot

ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-Dserver.port=${PORT}", "-jar", "/app/app.jar"]
