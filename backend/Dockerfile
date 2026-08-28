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

RUN apk add --no-cache git curl && \
    curl -sSL https://github.com/gitleaks/gitleaks/releases/download/v8.24.0/gitleaks_8.24.0_linux_x64.tar.gz -o gitleaks.tar.gz && \
    echo "cb49b7de5ee986510fe8666ca0273a6cc15eb82571f2f14832c9e8920751f3a4  gitleaks.tar.gz" | sha256sum -c - && \
    tar -xzf gitleaks.tar.gz -C /usr/local/bin/ gitleaks && \
    rm gitleaks.tar.gz && \
    chmod 755 /usr/local/bin/gitleaks

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

ENTRYPOINT ["sh", "-c", "exec java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
