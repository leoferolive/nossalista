# Story 1.3: Login com Email/Senha

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a usuário cadastrado,
I want fazer login com meu email e senha,
So that possa acessar minhas listas.

## Acceptance Criteria

**Given** o endpoint POST /api/auth/login está disponível
**When** envio credenciais válidas (email + senha)
**Then** response deve ser 200 OK
**And** body deve conter JWT token
**And** token deve ter expiração de 7 dias
**And** body deve conter dados do usuário (id, username, email, name, avatar_url)

**Given** o endpoint de login
**When** envio email inexistente
**Then** response deve ser 401 Unauthorized
**And** body deve seguir RFC 7807
**And** mensagem não deve revelar se email existe (segurança)

**Given** o endpoint de login
**When** envio senha incorreta
**Then** response deve ser 401 Unauthorized
**And** mensagem deve indicar credenciais inválidas

**Given** JWT token gerado
**When** decodificado
**Then** deve conter: user_id, email, username, exp (7 dias)
**And** deve ser assinado com chave secreta configurável

**Given** JWT token válido
**When** incluído no header Authorization: Bearer {token}
**Then** requests autenticados devem ser aceitos
**And** sistema deve extrair user_id do token

## Tasks / Subtasks

- [x] Task 1: Criar DTOs para Login (AC: Contratos API)
  - [x] 1.1: Criar LoginRequest em auth/dto/ com validações @NotBlank
  - [x] 1.2: Criar LoginResponse em auth/dto/ (campos do usuário + token + expiresAt)
  - [x] 1.3: Adicionar método toLoginResponse() no UserMapper
  - [x] 1.4: NÃO incluir password no response

- [x] Task 2: Criar JwtService para geração e validação de tokens (AC: JWT token)
  - [x] 2.1: Criar JwtService em auth/service/
  - [x] 2.2: Configurar chave secreta e tempo de expiração (7 dias) em application.yml
  - [x] 2.3: Implementar generateToken(User) → String usando JJWT 0.12.3
  - [x] 2.4: Token deve conter claims: sub=userId, email, username, exp
  - [x] 2.5: Implementar validateToken(String) → boolean
  - [x] 2.6: Implementar extractUserId(String) → UUID
  - [x] 2.7: Implementar getExpirationTime() → LocalDateTime

- [x] Task 3: Implementar AuthService.login (AC: Lógica de login)
  - [x] 3.1: Implementar método login(LoginRequest) em AuthService
  - [x] 3.2: Normalizar email (trim + toLowerCase)
  - [x] 3.3: Buscar usuário por email no UserRepository
  - [x] 3.4: Validar senha com passwordEncoder.matches(input, stored)
  - [x] 3.5: Lançar InvalidCredentialsException se usuário não existe OU senha incorreta
  - [x] 3.6: Retornar User se credenciais válidas

- [x] Task 4: Criar Exception para credenciais inválidas (AC: 401 Unauthorized)
  - [x] 4.1: Criar InvalidCredentialsException em auth/exception/
  - [x] 4.2: Mensagem genérica: "Email ou senha inválidos" (segurança)
  - [x] 4.3: Adicionar handler em GlobalExceptionHandler → 401 Unauthorized
  - [x] 4.4: Response deve seguir RFC 7807 Problem Details

- [x] Task 5: Implementar AuthController.login (AC: Endpoint POST /api/auth/login)
  - [x] 5.1: Implementar método login(@Valid @RequestBody LoginRequest) em AuthController
  - [x] 5.2: Chamar AuthService.login para validar credenciais
  - [x] 5.3: Chamar JwtService.generateToken para gerar JWT
  - [x] 5.4: Usar UserMapper.toLoginResponse para criar response
  - [x] 5.5: Retornar ResponseEntity com status 200 OK
  - [x] 5.6: Adicionar anotações SpringDoc (@Operation, @ApiResponse)

- [x] Task 6: Criar JwtAuthenticationFilter (AC: JWT token válido aceito)
  - [x] 6.1: Criar JwtAuthenticationFilter em config/ extends OncePerRequestFilter
  - [x] 6.2: Extrair token do header "Authorization: Bearer {token}"
  - [x] 6.3: Validar token com JwtService.validateToken()
  - [x] 6.4: Extrair userId com JwtService.extractUserId()
  - [x] 6.5: Carregar User do database via UserRepository
  - [x] 6.6: Criar UsernamePasswordAuthenticationToken e setar no SecurityContext
  - [x] 6.7: Se token inválido ou ausente, permitir que request continue (filtros posteriores tratam)

- [x] Task 7: Configurar JWT Filter no SecurityConfig (AC: Filtro ativo)
  - [x] 7.1: Injetar JwtAuthenticationFilter no SecurityConfig
  - [x] 7.2: Adicionar filter ANTES de UsernamePasswordAuthenticationFilter
  - [x] 7.3: Descomentar TODO na linha de addFilterBefore
  - [x] 7.4: Verificar que /api/auth/** permanece público

- [x] Task 8: Testes de Integração (AC: Validações funcionando)
  - [x] 8.1: Testar POST /api/auth/login com credenciais válidas → 200 OK + token
  - [x] 8.2: Testar POST com email inexistente → 401 Unauthorized
  - [x] 8.3: Testar POST com senha incorreta → 401 Unauthorized
  - [x] 8.4: Testar POST com campos vazios → 400 Bad Request
  - [x] 8.5: Verificar que token retornado é válido (decode com JwtService)
  - [x] 8.6: Verificar que password NÃO aparece no response
  - [x] 8.7: Testar request autenticado com JWT válido → 200 OK
  - [x] 8.8: Testar request com JWT inválido → 401 Unauthorized

## Dev Notes

### 🎯 Contexto da Story

Esta é a **TERCEIRA STORY** do Epic 1 (Autenticação). As Stories 1.1 e 1.2 já estabeleceram toda a infraestrutura de autenticação:
- **Story 1.1:** Configurou Spring Security, Flyway, PasswordEncoder (BCrypt)
- **Story 1.2:** Implementou registro de usuários com email/senha, validações, RFC 7807

**Objetivo Principal:** Permitir que usuários cadastrados façam login usando email e senha, recebendo um **JWT token stateless** que será usado para autenticar todas as próximas requisições.

Esta story implementa o **fluxo de autenticação completo** do NossaLista, incluindo geração de tokens JWT e filtro de autenticação que valida tokens em cada request.

### 🏗️ Decisões Arquiteturais Relevantes

**Decision #007: JWT Stateless Authentication (Architecture.md)**
- JWT é **OBRIGATÓRIO** para funcionar com WebSocket (Stories futuras)
- Tokens stateless eliminam necessidade de session storage
- Expiração de 7 dias conforme NFR-S3
- Claims mínimos: sub (userId), email, username, exp

**Decision #004: API Error Handling (Architecture.md)**
- **OBRIGATÓRIO** usar RFC 7807 Problem Details para erros
- Credenciais inválidas devem retornar 401 Unauthorized
- Mensagem genérica "Email ou senha inválidos" por segurança (não revelar se email existe)

**Story 1.2 - Registro Completo (já implementado):**
- UserRepository com métodos findByEmail() e findByUsername()
- PasswordEncoder (BCrypt) já configurado no SecurityConfig
- GlobalExceptionHandler preparado para adicionar novos handlers
- Pattern estabelecido: DTOs (Request/Response), Services, Controllers

**Cross-Cutting Concern: Authentication & Authorization (Architecture.md)**
- Afeta TODAS as APIs REST e endpoints WebSocket
- Implementação: JWT stateless com validação em cada request
- Criticidade: Alta - proteção de dados pessoais e controle de acesso

### 📦 Stack Técnico Específico

**Backend Components:**
- Spring Boot 4.0.2 + Java 25
- Spring Security para autenticação stateless
- **JJWT 0.12.3** para geração e validação de tokens JWT (já no pom.xml)
- Spring Validation (@Valid, @NotBlank)
- BCrypt para validação de senhas (via PasswordEncoder)

**Biblioteca JWT (JJWT 0.12.3):**
```xml
<!-- Já declarada no pom.xml da Story 1.2 -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
```

**Validações de Input:**
- Email/username: @NotBlank
- Senha: @NotBlank (sem validação de tamanho - já foi validado no registro)

### 🔐 JWT Token - Especificação Técnica

**Formato do Token JWT:**

O token JWT gerado seguirá o padrão RFC 7519 com os seguintes claims:

**Header:**
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

**Payload (Claims):**
```json
{
  "sub": "uuid-do-usuario",
  "email": "leo@example.com",
  "username": "leoferolive",
  "iat": 1707662400,
  "exp": 1708267200
}
```

**Signature:**
- Algoritmo: HS256 (HMAC SHA-256)
- Chave secreta: Configurável via `application.yml` → `jwt.secret`
- **IMPORTANTE:** Chave deve ter no mínimo 256 bits (32 bytes) para HS256

**Expiração:**
- Tempo de vida: **7 dias** (604800 segundos) conforme NFR-S3
- Campo `exp` (expiration time) em epoch segundos

**Geração de Token com JJWT 0.12.3:**

```java
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration:604800000}") // 7 dias em ms
    private long expirationMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
            .subject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("username", user.getUsername())
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(getSigningKey())
            .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public UUID extractUserId(String token) {
        String subject = Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject();
        return UUID.fromString(subject);
    }

    public LocalDateTime getExpirationTime() {
        long expiryTimestamp = System.currentTimeMillis() + expirationMs;
        return LocalDateTime.ofInstant(
            Instant.ofEpochMilli(expiryTimestamp),
            ZoneId.systemDefault()
        );
    }
}
```

**Configuração em application.yml:**

```yaml
jwt:
  secret: ${JWT_SECRET:change-this-secret-key-in-production-minimum-32-characters}
  expiration: 604800000  # 7 dias em milissegundos
```

**IMPORTANTE:**
- Chave secreta deve ser configurada via variável de ambiente `JWT_SECRET` em produção
- NUNCA commitar chave real no código
- Chave de desenvolvimento pode ficar no `application-dev.yml`

### 🔒 JwtAuthenticationFilter - Especificação

**Responsabilidade do Filtro:**
1. Interceptar TODAS as requests HTTP
2. Extrair token do header `Authorization: Bearer {token}`
3. Validar token com `JwtService`
4. Se válido: Carregar usuário e setar autenticação no `SecurityContext`
5. Se inválido ou ausente: Permitir que request continue (outros filtros/handlers lidam)

**Implementação:**

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7); // Remove "Bearer "

        if (!jwtService.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID userId = jwtService.extractUserId(token);
        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Criar autenticação
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                user,
                null,
                Collections.emptyList() // Authorities vazias por enquanto
            );

        authentication.setDetails(
            new WebAuthenticationDetailsSource().buildDetails(request)
        );

        // Setar no SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
```

**Integração no SecurityConfig:**

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/api/health", "/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            // ADICIONAR FILTER JWT
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ... resto do código existente
}
```

**IMPORTANTE:**
- Filtro é executado ANTES de `UsernamePasswordAuthenticationFilter`
- Endpoints `/api/auth/**` permanecem públicos (login e registro não precisam de token)
- Se token inválido, filtro não bloqueia - deixa outros filtros/handlers tratarem

### 🎨 Estrutura de Código Backend

**Novos Arquivos a Criar:**

```
backend/src/main/java/br/com/leoferolive/nossalista/
├── auth/
│   ├── dto/
│   │   ├── LoginRequest.java          # CRIAR
│   │   └── LoginResponse.java         # CRIAR
│   ├── service/
│   │   └── JwtService.java            # CRIAR
│   ├── exception/
│   │   └── InvalidCredentialsException.java  # CRIAR
│   └── controller/
│       └── AuthController.java        # MODIFICAR (adicionar endpoint /login)
├── config/
│   ├── JwtAuthenticationFilter.java   # CRIAR
│   ├── SecurityConfig.java            # MODIFICAR (adicionar filtro)
│   └── GlobalExceptionHandler.java    # MODIFICAR (handler para InvalidCredentialsException)
```

**Arquivos a Modificar:**

```
backend/src/main/java/br/com/leoferolive/nossalista/
├── auth/
│   ├── service/
│   │   └── AuthService.java           # Adicionar método login()
│   └── dto/
│       └── UserMapper.java            # Adicionar método toLoginResponse()
└── config/
    └── SecurityConfig.java            # Adicionar JwtAuthenticationFilter
```

**Configuração:**

```
backend/src/main/resources/
├── application.yml                     # MODIFICAR (adicionar jwt.secret e jwt.expiration)
└── application-dev.yml                # MODIFICAR (chave dev)
```

### 🔑 Autenticação e Segurança

**Fluxo de Login Completo:**

1. **Cliente envia credenciais:**
   ```http
   POST /api/auth/login
   Content-Type: application/json

   {
     "email": "leo@example.com",
     "password": "senha123"
   }
   ```

2. **Backend valida:**
   - AuthController recebe request
   - AuthService.login() busca usuário por email
   - PasswordEncoder.matches() valida senha hasheada
   - Se inválido: lança InvalidCredentialsException → 401
   - Se válido: continua

3. **Backend gera JWT:**
   - JwtService.generateToken(user) cria token
   - Token contém: userId, email, username, exp (7 dias)
   - Token é assinado com chave secreta

4. **Backend retorna response:**
   ```http
   HTTP/1.1 200 OK
   Content-Type: application/json

   {
     "id": "uuid-123",
     "username": "leoferolive",
     "email": "leo@example.com",
     "name": "Leonardo Oliveira",
     "avatarUrl": null,
     "authProvider": "EMAIL",
     "createdAt": "2026-02-11T10:00:00Z",
     "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     "expiresAt": "2026-02-18T10:00:00Z"
   }
   ```

5. **Cliente armazena token:**
   - Frontend salva token em localStorage ou sessionStorage
   - Inclui em todas as próximas requests: `Authorization: Bearer {token}`

6. **Requests autenticados:**
   ```http
   GET /api/lists
   Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
   ```

7. **JwtAuthenticationFilter valida:**
   - Extrai token do header
   - Valida assinatura e expiração
   - Carrega usuário do database
   - Seta autenticação no SecurityContext
   - Request prossegue autenticado

**Segurança - Checklist:**

- ✅ Mensagem genérica para credenciais inválidas (não vazar se email existe)
- ✅ Senha validada com BCrypt (tempo constante, resiste a timing attacks)
- ✅ Token JWT assinado com chave secreta forte (min 256 bits)
- ✅ Expiração de 7 dias (NFR-S3)
- ✅ Chave secreta configurável via environment variable
- ✅ Password NUNCA retornado em responses
- ✅ HTTPS obrigatório em produção (via Cloudflare Tunnel - NFR-S1)
- ✅ CORS configurado para domínio único

### 📝 RFC 7807 Problem Details - Erro de Login

**Exemplo de erro 401 Unauthorized (credenciais inválidas):**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCredentials(
        InvalidCredentialsException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED,
            ex.getMessage()
        );
        problem.setType(URI.create("https://api.nossalista.com/docs/errors/invalid-credentials"));
        problem.setTitle("Credenciais inválidas");
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }
}
```

**Response esperado:**

```json
{
  "type": "https://api.nossalista.com/docs/errors/invalid-credentials",
  "title": "Credenciais inválidas",
  "status": 401,
  "detail": "Email ou senha inválidos",
  "instance": "/api/auth/login"
}
```

**IMPORTANTE:**
- Mensagem genérica "Email ou senha inválidos" para AMBOS os casos (email inexistente OU senha incorreta)
- Não vazar informação sobre existência de email (segurança)

### 🧪 Testes Manuais (via curl ou Swagger UI)

**1. Login com sucesso:**

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "leo@example.com",
    "password": "senha123"
  }'

# Expected: 200 OK
# Response:
{
  "id": "uuid-123",
  "username": "leoferolive",
  "email": "leo@example.com",
  "name": "Leonardo Oliveira",
  "avatarUrl": null,
  "authProvider": "EMAIL",
  "createdAt": "2026-02-11T10:00:00Z",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresAt": "2026-02-18T10:00:00Z"
}
```

**2. Email inexistente:**

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "naoexiste@example.com",
    "password": "senha123"
  }'

# Expected: 401 Unauthorized
# Response: RFC 7807 Problem Details
{
  "type": "https://api.nossalista.com/docs/errors/invalid-credentials",
  "title": "Credenciais inválidas",
  "status": 401,
  "detail": "Email ou senha inválidos",
  "instance": "/api/auth/login"
}
```

**3. Senha incorreta:**

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "leo@example.com",
    "password": "senhaerrada"
  }'

# Expected: 401 Unauthorized (mesma resposta do teste 2)
```

**4. Request autenticado com JWT:**

```bash
# Primeiro, fazer login e pegar o token
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Usar token em request protegido
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $TOKEN"

# Expected: 200 OK com dados do usuário
```

**5. Token inválido:**

```bash
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer token-invalido"

# Expected: 401 Unauthorized
```

**6. Verificar token gerado:**

Decodificar token JWT em https://jwt.io/ e verificar:
- Header: `{ "alg": "HS256", "typ": "JWT" }`
- Payload: sub (userId), email, username, iat, exp
- Assinatura válida (quando chave secreta inserida)

### 🚨 Armadilhas Comuns a Evitar

1. **NUNCA retornar mensagem específica** - "Email não encontrado" vaza informação. Sempre usar "Email ou senha inválidos"
2. **NÃO esquecer @Valid** - Controller deve ter `@Valid @RequestBody` para validações funcionarem
3. **NÃO usar string pequena como chave JWT** - Mínimo 256 bits (32 bytes) para HS256
4. **ATENÇÃO à ordem dos filtros** - JwtAuthenticationFilter ANTES de UsernamePasswordAuthenticationFilter
5. **NÃO bloquear request no filtro** - Se token inválido, deixar continuar (outros filtros/handlers tratam)
6. **NÃO validar tamanho de senha no login** - Usuário pode ter senha válida cadastrada (validação só no registro)
7. **IMPORTANTE:** Normalizar email (trim + toLowerCase) igual ao registro
8. **CUIDADO com timezone** - Usar UTC para timestamps ou LocalDateTime com ZoneId

### 🔗 Relacionamento com Outras Stories

**Depende de:**
- Story 1.1: Setup completo (SecurityConfig, Flyway, PasswordEncoder) ✅
- Story 1.2: Registro de usuários (User entity, UserRepository, AuthService base) ✅

**Próximas Stories usarão:**
- Story 1.4: OAuth2 vai gerar JWT token da mesma forma (JwtService reutilizado)
- Story 1.5: Perfil vai usar JwtAuthenticationFilter para identificar usuário autenticado
- Story 2.1+: Todas as stories de listas usarão JWT para autenticação

**Esta story habilita:**
- ✅ Autenticação stateless em TODAS as APIs REST
- ✅ Identificação de usuário logado via SecurityContext
- ✅ Fundação para WebSocket authentication (Stories 5.x)

### 📊 Critérios de Aceitação - Checklist

Antes de marcar esta story como completa, verificar:

✅ JwtService criado e configurado com JJWT 0.12.3
✅ JwtService.generateToken() retorna token válido com claims corretos
✅ JwtService.validateToken() valida assinatura e expiração
✅ JwtService.extractUserId() extrai UUID do subject
✅ POST /api/auth/login retorna 200 OK com token válido
✅ Response contém: id, username, email, name, avatarUrl, authProvider, createdAt, token, expiresAt
✅ Response NÃO contém campo password
✅ Email inexistente retorna 401 com mensagem genérica
✅ Senha incorreta retorna 401 com mensagem genérica
✅ Campos vazios retornam 400 Bad Request com erros de validação
✅ JwtAuthenticationFilter criado e registrado no SecurityConfig
✅ Request com JWT válido é aceito e usuário identificado
✅ Request com JWT inválido/expirado é rejeitado
✅ Swagger UI documenta endpoint /api/auth/login
✅ Testes de integração cobrem todos os cenários
✅ Chave JWT configurada em application.yml

### Project Structure Notes

**Alinhamento com Estrutura de Projeto Unificada:**

Esta story expande o módulo de autenticação criado na Story 1.2, adicionando componentes críticos de segurança.

**Novos Arquivos/Pastas Criados:**

```
backend/src/main/java/br/com/leoferolive/nossalista/
├── auth/
│   ├── dto/
│   │   ├── LoginRequest.java          # CRIAR
│   │   └── LoginResponse.java         # CRIAR
│   ├── service/
│   │   └── JwtService.java            # CRIAR
│   └── exception/
│       └── InvalidCredentialsException.java  # CRIAR
├── config/
│   └── JwtAuthenticationFilter.java   # CRIAR
```

**Convenções de Nomenclatura:**

- Services: `JwtService` (específico para JWT)
- Filters: `<Purpose>Filter` (JwtAuthenticationFilter)
- DTOs: `<Operation>Request/Response` (LoginRequest, LoginResponse)
- Exceptions: `<Context>Exception` (InvalidCredentialsException)

**Padrões de Código Backend Estabelecidos:**

- **JwtService:** @Service com métodos utilitários para JWT
- **Filter:** Extends OncePerRequestFilter, registrado em SecurityConfig
- **LoginRequest/Response:** Records Java com validações e campos imutáveis
- **Exception Handler:** @ExceptionHandler em GlobalExceptionHandler → RFC 7807

**Configuração:**

- **application.yml:** Seção `jwt:` com `secret` e `expiration`
- **application-dev.yml:** Chave de desenvolvimento (não usar em prod)

### References

Todos os detalhes técnicos com fontes de documentação:

**Epics e Stories:**
- [Fonte: _bmad-output/planning-artifacts/epics.md#Story-1.3]
  - Epic 1: Autenticação e Perfis de Usuário
  - Story 1.3: Login com Email/Senha (linhas 446-482)
  - Acceptance Criteria completos
  - Próximas stories: 1.4 (OAuth2), 1.5 (Perfil)

**Decisões Arquiteturais:**
- [Fonte: _bmad-output/planning-artifacts/architecture.md]
  - Decision #007: JWT stateless authentication (sem servidor de sessão)
  - Decision #004: API Error Handling Standard - RFC 7807 obrigatório
  - Cross-Cutting Concern: Authentication & Authorization (JWT em cada request)
  - NFR-S3: JWT expiração máxima de 7 dias

**Story Anterior:**
- [Fonte: _bmad-output/implementation-artifacts/1-2-registro-com-email-senha.md]
  - UserRepository com findByEmail() implementado
  - PasswordEncoder (BCrypt) configurado no SecurityConfig
  - GlobalExceptionHandler com pattern RFC 7807 estabelecido
  - Pattern de DTOs (Request/Response), Services, Controllers
  - Normalização de inputs (trim, toLowerCase)

**Padrões de Código Identificados (Análise Story 1.2):**
- Constructor Injection sem @Autowired
- Records Java 21+ para DTOs imutáveis
- @Valid em @RequestBody para ativar validações
- PasswordEncoder.matches() para validar senhas
- Exceções customizadas que estendem RuntimeException
- Testes BDD (Given/When/Then)
- Javadoc em português para classes e métodos públicos
- Messages de validação em português

**Documentação Técnica Externa:**
- JJWT (Java JWT): https://github.com/jwtk/jjwt
- Spring Security JWT: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
- RFC 7519 (JWT): https://www.rfc-editor.org/rfc/rfc7519
- RFC 7807 Problem Details: https://www.rfc-editor.org/rfc/rfc7807
- BCrypt Password Validation: https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html

## Dev Agent Record

### Agent Model Used

Claude Sonnet 4.5 (claude-sonnet-4-5-20250929)

### Debug Log References

Nenhum debug log necessário. Implementação rodou sem erros críticos.

### Completion Notes List

**2026-02-11: Story 1.3 Implementada e Testada**

- ✅ **Todas as 8 tasks completadas** (45 subtasks no total)
- ✅ **Todos os 10 Acceptance Criteria implementados e validados**
- ✅ **16 testes de integração passando** (8 originais de registro + 8 novos de login)
- ✅ **JwtService criado** com JJWT 0.12.3 (geração, validação, extração de userId)
- ✅ **JwtAuthenticationFilter configurado** no SecurityConfig (executa ANTES de UsernamePasswordAuthenticationFilter)
- ✅ **Configuração JWT** adicionada em application.yml e application-dev.yml
- ✅ **Endpoint POST /api/auth/login** implementado e documentado com SpringDoc
- ✅ **InvalidCredentialsException** com mensagem genérica (segurança: não vaza se email existe)
- ✅ **AuthService.login()** com normalização de email (trim + toLowerCase)
- ✅ **Password NUNCA retornado** no LoginResponse (validado em testes)
- ✅ **Token JWT válido** contém claims: sub (userId), email, username, iat, exp
- ✅ **Expiração de 7 dias** configurada (604800000 ms)
- ✅ **Todas as validações funcionando**: 401 para email inexistente, 401 para senha incorreta, 400 para campos vazios

**Decisões de Implementação:**
- Chave JWT mínimo 32 caracteres para HS256 (configurada em application.yml)
- Mensagem genérica "Email ou senha inválidos" para email inexistente OU senha incorreta (segurança)
- JwtAuthenticationFilter permite continuação do request se token inválido (outros filtros/handlers lidam)
- Endpoints /api/auth/** permanecem públicos (login e registro não precisam de token)

**Próximas Stories Habilitadas:**
- Story 1.4: OAuth2 Login (reutilizará JwtService)
- Story 1.5: Perfil do Usuário (usará JwtAuthenticationFilter para identificar usuário)
- Stories 2.x: Listas (todas autenticadas via JWT)

### File List

**Arquivos Criados:**
- backend/src/main/java/br/com/leoferolive/nossalista/auth/dto/LoginRequest.java
- backend/src/main/java/br/com/leoferolive/nossalista/auth/dto/LoginResponse.java
- backend/src/main/java/br/com/leoferolive/nossalista/auth/service/JwtService.java
- backend/src/main/java/br/com/leoferolive/nossalista/auth/exception/InvalidCredentialsException.java
- backend/src/main/java/br/com/leoferolive/nossalista/config/JwtAuthenticationFilter.java

**Arquivos Modificados:**
- backend/src/main/java/br/com/leoferolive/nossalista/auth/dto/UserMapper.java (adicionado método toLoginResponse)
- backend/src/main/java/br/com/leoferolive/nossalista/auth/service/AuthService.java (adicionado método login)
- backend/src/main/java/br/com/leoferolive/nossalista/auth/controller/AuthController.java (adicionado endpoint /login)
- backend/src/main/java/br/com/leoferolive/nossalista/config/GlobalExceptionHandler.java (adicionado handler InvalidCredentialsException)
- backend/src/main/java/br/com/leoferolive/nossalista/config/SecurityConfig.java (adicionado JwtAuthenticationFilter)
- backend/src/main/resources/application.yml (adicionada configuração jwt.secret e jwt.expiration)
- backend/src/main/resources/application-dev.yml (adicionada chave JWT de desenvolvimento)
- backend/src/test/java/br/com/leoferolive/nossalista/auth/controller/AuthControllerTest.java (adicionados 8 testes de login)
