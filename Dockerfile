# Multi-stage Dockerfile para Spring Boot
# Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copiar pom.xml e baixar dependências (cache layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código fonte e build
COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Criar usuário non-root para segurança
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Copiar JAR da build stage
COPY --from=builder /app/target/*.jar app.jar

# Trocar para usuário non-root
USER appuser

# Expor porta
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
