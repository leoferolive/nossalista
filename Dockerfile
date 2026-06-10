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
# Seleciona explicitamente o jar executavel (repackaged pelo spring-boot-maven-plugin),
# excluindo o `*.jar.original` (jar plano sem launcher) e quaisquer jars de plugins
# que possam ser copiados para target/. Mesma logica do smoke test do CI.
RUN APP_JAR="$(ls target/nossalista-*.jar | grep -v '\.original$' | head -1)" \
    && cp "$APP_JAR" /app/app.jar

# Stage 3: Runtime mínimo
FROM eclipse-temurin:25-jre
WORKDIR /app
# eclipse-temurin:25-jre é baseado em Ubuntu e NÃO inclui curl nem wget por padrão.
# Instala curl minimamente para o HEALTHCHECK (sem isso o healthcheck falharia sempre).
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
RUN groupadd -r appuser && useradd -r -g appuser appuser
ARG APP_VERSION=local-dev
ARG APP_GIT_SHA=unknown
ARG APP_GIT_TAG=unknown
ARG APP_BUILD_TIME=unknown
ARG APP_ENVIRONMENT=local
ENV APP_VERSION=$APP_VERSION \
    APP_GIT_SHA=$APP_GIT_SHA \
    APP_GIT_TAG=$APP_GIT_TAG \
    APP_BUILD_TIME=$APP_BUILD_TIME \
    APP_ENVIRONMENT=$APP_ENVIRONMENT
COPY --from=backend-builder /app/app.jar app.jar
USER appuser
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
