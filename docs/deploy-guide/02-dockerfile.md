# Dockerfile — Build Multi-Stage

## 1. Problema Atual

O `Dockerfile` na raiz do repositório (estado inicial):
- Copia `pom.xml` e `src/` diretamente — incompatível com a estrutura de monorepo (`backend/` e `frontend/` separados)
- Não inclui o build do frontend
- Resultado: o container serve apenas a API, sem o SPA React

## 2. Dockerfile Novo (Substituir o Arquivo Raiz)

```dockerfile
# Stage 1: Build do Frontend
FROM node:22-alpine AS frontend-builder
WORKDIR /app
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ .
RUN npm run build

# Stage 2: Build do Backend + Frontend embutido
FROM maven:3.9-eclipse-temurin-25 AS backend-builder
WORKDIR /app
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B
COPY backend/src ./src
# Embute o frontend no classpath do Spring Boot (servido como static resources)
COPY --from=frontend-builder /app/dist ./src/main/resources/static/
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
```

## 3. Decisões do Dockerfile

| Decisão | Motivo |
|---------|--------|
| `node:22-alpine` | Alpine minimiza tamanho; Node 22 é LTS atual |
| `maven:3.9-eclipse-temurin-25` | Maven 3.9 + JDK 25 compatível com Spring Boot 4 |
| `eclipse-temurin:25-jre` | JRE (não JDK) minimiza imagem final |
| `RUN mvn dependency:go-offline` | Cache de dependências Maven em camada separada |
| `USER appuser` | Segurança: não rodar como root |
| `HEALTHCHECK` | K8s usa `/actuator/health` para liveness, mas Docker usa este |

## 4. Verificação do Build Local

```bash
# Da raiz do monorepo (onde está o Dockerfile)
docker build -t nossalista:local .

# Verificar tamanho da imagem final
docker images nossalista:local

# Testar container localmente
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e DATABASE_URL=jdbc:postgresql://192.168.3.63:30001/nossalista_dev \
  -e DATABASE_USER=nossalista_dev \
  -e DATABASE_PASSWORD=<senha-dev> \
  -e JWT_SECRET=<secret-min-32-chars> \
  -e GOOGLE_CLIENT_ID=<client-id> \
  -e GOOGLE_CLIENT_SECRET=<client-secret> \
  -e FRONTEND_URL=http://localhost:8080 \
  nossalista:local

# Verificações após inicialização (~60s para Spring Boot)
curl http://localhost:8080/actuator/health
# Esperado: {"status":"UP"}

curl http://localhost:8080/
# Esperado: HTML do React SPA (index.html)

curl http://localhost:8080/api/health
# Esperado: 200 OK (se endpoint existir) ou 404 esperado
```

## 5. Build para ARM64 (Raspberry Pi)

O GitHub Actions faz o build para ARM64 automaticamente via QEMU. Para build local em máquina x86:

```bash
# Habilitar buildx para ARM64
docker buildx create --use
docker buildx build --platform linux/arm64 -t nossalista:arm64 . --load

# Ou fazer push direto
docker buildx build --platform linux/arm64 \
  -t ghcr.io/leoferolive/nossalista-dev:latest \
  --push .
```

## 6. Camadas e Cache

O Dockerfile é otimizado para cache:

1. `COPY frontend/package*.json` + `npm ci` — cache enquanto `package.json` não mudar
2. `COPY frontend/` + `npm run build` — invalida apenas quando código frontend muda
3. `COPY backend/pom.xml` + `mvn dependency:go-offline` — cache de dependências Maven
4. `COPY backend/src` + `mvn package` — invalida apenas quando código backend muda
