# Stage 1: Build do Frontend (roda nativo no runner, sem emulação QEMU)
FROM --platform=$BUILDPLATFORM node:22-alpine AS frontend-builder
WORKDIR /workspace
COPY frontend/package*.json frontend/
RUN npm ci --prefix frontend
COPY frontend/ frontend/
COPY contracts/ contracts/
ARG GIT_SHA=unknown
RUN npm run build --prefix frontend

# Stage 2: Build do Backend + Frontend embutido (roda nativo no runner, sem emulação QEMU)
FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-25 AS backend-builder
WORKDIR /app
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B
COPY backend/src ./src
COPY --from=frontend-builder /workspace/frontend/dist ./src/main/resources/static/
RUN mvn clean package -DskipTests -B

# Stage 3: Runtime mínimo
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN groupadd -r appuser && useradd -r -g appuser appuser
COPY --from=backend-builder /app/target/*.jar app.jar
USER appuser
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
