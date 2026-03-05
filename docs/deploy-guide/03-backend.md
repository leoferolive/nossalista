# Backend — Mudanças Necessárias

## 1. Servir o SPA do Spring Boot (SpaController)

Spring Boot serve automaticamente arquivos de `classpath:/static/`. O arquivo `dist/index.html` (copiado pelo Dockerfile) será servido em `/`.

**Problema com React Router:** Rotas como `/listas/123` ou `/perfil` retornam 404 porque o Spring Boot tenta encontrar um arquivo correspondente. É necessário um fallback para `index.html`.

### Verificar se já existe

```bash
find backend/src -name "SpaController.java" -o -name "SpaWebFilter.java" 2>/dev/null
```

### Criar se não existir

**Arquivo:** `backend/src/main/java/br/com/leoferolive/nossalista/config/SpaController.java`

```java
package br.com.leoferolive.nossalista.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Redireciona rotas do React Router para index.html.
 * Exclui rotas /api/**, /ws/**, /actuator/** e arquivos com extensão.
 */
@Controller
public class SpaController {

    @GetMapping(value = {
        "/",
        "/{path:[^\\.]*}",
        "/{path:^(?!api|ws|actuator).*$}/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
```

**Por que esse padrão de regex:**
- `/{path:[^\\.]*}` — rotas sem extensão (ex: `/home`, `/listas`)
- `/{path:^(?!api|ws|actuator).*$}/**` — subpaths que não começam com `api`, `ws` ou `actuator`
- Arquivos com extensão (`.js`, `.css`, `.png`) continuam sendo servidos diretamente pelo `ResourceHttpRequestHandler`

## 2. application-dev.yml — Migrar para Env Vars

### Situação Atual (hardcoded — incompatível com K8s)

```yaml
# backend/src/main/resources/application-dev.yml (atual)
spring:
  datasource:
    url: jdbc:postgresql://192.168.3.63:30001/nossalista_dev
    username: nossalista_dev
    password: nossalistadev
```

### Configuração Nova (env vars com fallback para dev local)

```yaml
# backend/src/main/resources/application-dev.yml (novo)
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/nossalista_dev}
    username: ${DATABASE_USER:nossalista_dev}
    password: ${DATABASE_PASSWORD:nossalistadev}
  jpa:
    hibernate:
      ddl-auto: validate

# JWT e OAuth também devem vir de env vars
jwt:
  secret: ${JWT_SECRET:dev-secret-change-in-production-min-32-chars}

spring.security.oauth2.client.registration.google:
  client-id: ${GOOGLE_CLIENT_ID:}
  client-secret: ${GOOGLE_CLIENT_SECRET:}
  redirect-uri: ${FRONTEND_URL:http://localhost:8080}/api/auth/google/callback

frontend:
  url: ${FRONTEND_URL:http://localhost:8080}
```

**Benefício:** A mesma imagem Docker funciona em dev local (sem env vars → usa defaults), em K8s dev (env vars do secret `nossalista-secrets`) e em K8s prod.

## 3. application-prod.yml — Verificar Consistência

Confirmar que `application-prod.yml` já usa env vars e é consistente com os secrets K8s planejados:

```yaml
# Deve conter (sem valores hardcoded):
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USER}
    password: ${DATABASE_PASSWORD}

jwt:
  secret: ${JWT_SECRET}

frontend:
  url: ${FRONTEND_URL:https://nossalista.leoferolive.com.br}
```

```bash
# Verificar:
cat backend/src/main/resources/application-prod.yml
```

## 4. CORS — Frontend Embutido

Com o frontend embutido no JAR, as requisições são **same-origin** em produção (não há CORS). Entretanto, o `CorsConfig` ainda é necessário para:

- **Dev local:** `npm run dev` roda na porta 5173, Spring Boot na 8080 → cross-origin
- **Google OAuth redirect:** o `frontend.url` é usado na configuração do OAuth

### Verificar CorsConfig

```bash
find backend/src -name "CorsConfig.java" -o -name "WebConfig.java" 2>/dev/null
```

O `CorsConfig` deve aceitar:
- `http://localhost:5173` (dev local com Vite)
- `http://localhost:8080` (dev local com container)
- `http://nossalista.home` (ambiente dev K8s)
- `https://nossalista.leoferolive.com.br` (ambiente prod K8s)

### Exemplo de configuração

```java
@Configuration
public class CorsConfig {

    @Value("${frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
            frontendUrl,
            "http://localhost:5173",
            "http://localhost:8080"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        // ...
    }
}
```

## 5. Checklist de Verificação do Backend

```bash
# 1. Verificar SpaController
find backend/src -name "SpaController.java"

# 2. Verificar application-dev.yml (não deve ter IPs hardcoded)
grep -n "192.168" backend/src/main/resources/application-dev.yml
# Esperado: sem resultados

# 3. Verificar application-prod.yml
cat backend/src/main/resources/application-prod.yml

# 4. Build local do backend para verificar compilação
cd backend && mvn clean package -DskipTests
# Esperado: BUILD SUCCESS
```
