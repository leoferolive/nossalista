# Login por Magic Link — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar login sem senha por magic link para contas já existentes, conectando o código órfão `EmailService.sendMagicLink`.

**Architecture:** Espelha o padrão de token stateful já usado em reset de senha / verificação de e-mail: tabela própria (`magic_link_tokens`), token opaco UUID de uso único (validade 10 min), `MagicLinkService` para solicitar/consumir, dois endpoints REST (`POST /magic-link` anti-enumeração; `POST /magic-login` que emite JWT). No frontend, botão secundário no `LoginModal` para solicitar e página nova `/magic-login` para consumir (espelhando `AuthCallback`).

**Tech Stack:** Java 25 + Spring Boot 4, Spring Data JPA, Flyway; React 19 + TypeScript + Vite, Vitest, axios.

**Referência:** `docs/superpowers/specs/2026-07-03-magic-link-login-design.md`

---

## Estrutura de arquivos

**Backend — criar:**
- `backend/src/main/resources/db/migration/V15__create_magic_link_tokens.sql`
- `backend/src/main/java/br/com/leoferolive/nossalista/auth/domain/MagicLinkToken.java`
- `backend/src/main/java/br/com/leoferolive/nossalista/auth/repository/MagicLinkTokenRepository.java`
- `backend/src/main/java/br/com/leoferolive/nossalista/auth/exception/InvalidMagicLinkTokenException.java`
- `backend/src/main/java/br/com/leoferolive/nossalista/auth/service/MagicLinkService.java`
- `backend/src/main/java/br/com/leoferolive/nossalista/auth/dto/MagicLinkRequest.java`
- `backend/src/main/java/br/com/leoferolive/nossalista/auth/dto/MagicLoginRequest.java`
- `backend/src/test/java/br/com/leoferolive/nossalista/auth/service/MagicLinkServiceTest.java`
- `backend/src/test/java/br/com/leoferolive/nossalista/auth/controller/MagicLinkControllerTest.java`

**Backend — modificar:**
- `backend/src/main/java/br/com/leoferolive/nossalista/auth/controller/AuthController.java` (injeção + 2 endpoints + constantes de rate limit)
- `backend/src/main/java/br/com/leoferolive/nossalista/config/GlobalExceptionHandler.java` (handler 400)

**Frontend — criar:**
- `frontend/src/pages/MagicLogin.tsx`
- `frontend/src/pages/MagicLogin.test.tsx`

**Frontend — modificar:**
- `frontend/src/api/authApi.ts` (2 métodos)
- `frontend/src/components/LoginModal.tsx` (botão + handler)
- `frontend/src/components/LoginModal.test.tsx` (teste do botão)
- `frontend/src/main.tsx` (rota `/magic-login`)

**Docs — modificar:**
- `docs/DECISIONS.md` (D-025)
- `docs/auth-endpoints-matrix.md` (2 linhas novas)
- `backend/.env.example` / `README.md` / `frontend/README.md` (se enumerarem fluxos/rotas)

**Comandos de referência** (rodar dentro do diretório indicado):
- Backend um teste: `cd backend && ./mvnw -q test -Dtest=NomeDoTeste`
- Frontend testes: `cd frontend && npm test -- --run <arquivo>`
- Quality gate pré-commit: `./scripts/quality.sh --pre-commit`
- Prettier (o pre-commit NÃO cobre): `cd frontend && npm run format:check`

---

## Task 1: Backend — migration + domínio (token, repositório, exceção)

**Files:**
- Create: `backend/src/main/resources/db/migration/V15__create_magic_link_tokens.sql`
- Create: `backend/src/main/java/br/com/leoferolive/nossalista/auth/domain/MagicLinkToken.java`
- Create: `backend/src/main/java/br/com/leoferolive/nossalista/auth/repository/MagicLinkTokenRepository.java`
- Create: `backend/src/main/java/br/com/leoferolive/nossalista/auth/exception/InvalidMagicLinkTokenException.java`

> Scaffolding de infra (espelha `PasswordResetToken`/`PasswordResetTokenRepository`/`InvalidResetTokenException`). Sem teste unitário próprio — validado pelo boot do schema e pelos testes das Tasks 2–3, seguindo o padrão do projeto (o reset de senha também não tem teste de repositório dedicado).

- [ ] **Step 1: Criar a migration V15**

`backend/src/main/resources/db/migration/V15__create_magic_link_tokens.sql`:

```sql
CREATE TABLE magic_link_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_magic_link_tokens_token ON magic_link_tokens(token);
CREATE INDEX idx_magic_link_tokens_user_id ON magic_link_tokens(user_id);
```

- [ ] **Step 2: Criar a entidade `MagicLinkToken`**

`auth/domain/MagicLinkToken.java` — cópia estrutural de `PasswordResetToken`, apenas trocando o nome da tabela/classe:

```java
package br.com.leoferolive.nossalista.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade que representa um token de login por magic link.
 * Tokens expiram após 10 minutos e só podem ser usados uma vez.
 */
@Entity
@Table(name = "magic_link_tokens")
public class MagicLinkToken {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used", nullable = false)
    private boolean used = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
    }

    public MagicLinkToken() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 3: Criar o repositório**

`auth/repository/MagicLinkTokenRepository.java`:

```java
package br.com.leoferolive.nossalista.auth.repository;

import br.com.leoferolive.nossalista.auth.domain.MagicLinkToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MagicLinkTokenRepository extends JpaRepository<MagicLinkToken, UUID> {

    Optional<MagicLinkToken> findByTokenAndUsedFalse(String token);

    void deleteByUserIdAndUsedFalse(UUID userId);
}
```

- [ ] **Step 4: Criar a exceção**

`auth/exception/InvalidMagicLinkTokenException.java`:

```java
package br.com.leoferolive.nossalista.auth.exception;

/**
 * Exceção lançada quando um token de magic link é inválido, expirado ou já usado.
 */
public class InvalidMagicLinkTokenException extends RuntimeException {

    public InvalidMagicLinkTokenException(String message) {
        super(message);
    }
}
```

- [ ] **Step 5: Compilar**

Run: `cd backend && ./mvnw -q test-compile`
Expected: BUILD SUCCESS (sem erros de compilação).

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/resources/db/migration/V15__create_magic_link_tokens.sql \
        backend/src/main/java/br/com/leoferolive/nossalista/auth/domain/MagicLinkToken.java \
        backend/src/main/java/br/com/leoferolive/nossalista/auth/repository/MagicLinkTokenRepository.java \
        backend/src/main/java/br/com/leoferolive/nossalista/auth/exception/InvalidMagicLinkTokenException.java
git commit -m "feat(auth): tabela e dominio do token de magic link"
```

---

## Task 2: Backend — `MagicLinkService` (TDD)

**Files:**
- Create: `backend/src/main/java/br/com/leoferolive/nossalista/auth/service/MagicLinkService.java`
- Test: `backend/src/test/java/br/com/leoferolive/nossalista/auth/service/MagicLinkServiceTest.java`

- [ ] **Step 1: Escrever o teste que falha**

`auth/service/MagicLinkServiceTest.java` (espelha `PasswordResetServiceTest`):

```java
package br.com.leoferolive.nossalista.auth.service;

import br.com.leoferolive.nossalista.auth.domain.MagicLinkToken;
import br.com.leoferolive.nossalista.auth.exception.InvalidMagicLinkTokenException;
import br.com.leoferolive.nossalista.auth.repository.MagicLinkTokenRepository;
import br.com.leoferolive.nossalista.email.service.EmailService;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MagicLinkService")
class MagicLinkServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private MagicLinkTokenRepository tokenRepository;

    @Mock
    private EmailService emailService;

    private MagicLinkService magicLinkService;

    @BeforeEach
    void setUp() {
        magicLinkService = new MagicLinkService(userService, tokenRepository, emailService);
    }

    private User user(AuthProvider provider) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail("ana@test.com");
        u.setName("Ana");
        u.setAuthProvider(provider);
        return u;
    }

    @Test
    @DisplayName("requestMagicLink envia e-mail e cria token quando usuário existe")
    void requestSendsWhenUserExists() {
        User u = user(AuthProvider.EMAIL);
        when(userService.findByEmailOptional("ana@test.com")).thenReturn(Optional.of(u));
        when(tokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        magicLinkService.requestMagicLink("ana@test.com");

        verify(tokenRepository).deleteByUserIdAndUsedFalse(u.getId());
        verify(tokenRepository).save(any(MagicLinkToken.class));
        verify(emailService).sendMagicLink(eq("ana@test.com"), eq("Ana"), anyString());
    }

    @Test
    @DisplayName("requestMagicLink funciona para conta OAuth (não filtra provider)")
    void requestWorksForOAuthAccount() {
        User u = user(AuthProvider.GOOGLE);
        when(userService.findByEmailOptional("ana@test.com")).thenReturn(Optional.of(u));
        when(tokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        magicLinkService.requestMagicLink("ana@test.com");

        verify(emailService).sendMagicLink(eq("ana@test.com"), eq("Ana"), anyString());
    }

    @Test
    @DisplayName("requestMagicLink é no-op quando usuário não existe")
    void requestNoOpWhenUserMissing() {
        when(userService.findByEmailOptional("nao@test.com")).thenReturn(Optional.empty());

        magicLinkService.requestMagicLink("nao@test.com");

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendMagicLink(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("requestMagicLink não propaga falha de envio de e-mail")
    void requestDoesNotPropagateEmailFailure() {
        User u = user(AuthProvider.EMAIL);
        when(userService.findByEmailOptional("ana@test.com")).thenReturn(Optional.of(u));
        when(tokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doThrow(new RuntimeException("SMTP down"))
            .when(emailService).sendMagicLink(anyString(), anyString(), anyString());

        assertThatCode(() -> magicLinkService.requestMagicLink("ana@test.com"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("consume válido marca token usado, verifica e-mail e retorna o usuário")
    void consumeValid() {
        UUID userId = UUID.randomUUID();
        MagicLinkToken token = new MagicLinkToken();
        token.setUserId(userId);
        token.setToken("valid");
        token.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        token.setUsed(false);
        User u = new User();
        u.setId(userId);
        u.setEmail("ana@test.com");

        when(tokenRepository.findByTokenAndUsedFalse("valid")).thenReturn(Optional.of(token));
        when(userService.findById(userId)).thenReturn(Optional.of(u));

        User result = magicLinkService.consume("valid");

        assertThat(token.isUsed()).isTrue();
        verify(tokenRepository).save(token);
        verify(userService).markEmailVerified(userId);
        assertThat(result).isEqualTo(u);
    }

    @Test
    @DisplayName("consume lança para token inválido")
    void consumeInvalid() {
        when(tokenRepository.findByTokenAndUsedFalse("x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> magicLinkService.consume("x"))
            .isInstanceOf(InvalidMagicLinkTokenException.class);
    }

    @Test
    @DisplayName("consume lança para token expirado")
    void consumeExpired() {
        MagicLinkToken token = new MagicLinkToken();
        token.setUserId(UUID.randomUUID());
        token.setToken("old");
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        token.setUsed(false);
        when(tokenRepository.findByTokenAndUsedFalse("old")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> magicLinkService.consume("old"))
            .isInstanceOf(InvalidMagicLinkTokenException.class);
        verify(userService, never()).markEmailVerified(any());
    }
}
```

- [ ] **Step 2: Rodar o teste e confirmar que falha**

Run: `cd backend && ./mvnw -q test -Dtest=MagicLinkServiceTest`
Expected: FALHA de compilação/execução — `MagicLinkService` ainda não existe.

- [ ] **Step 3: Implementar `MagicLinkService`**

`auth/service/MagicLinkService.java`:

```java
package br.com.leoferolive.nossalista.auth.service;

import br.com.leoferolive.nossalista.auth.domain.MagicLinkToken;
import br.com.leoferolive.nossalista.auth.exception.InvalidMagicLinkTokenException;
import br.com.leoferolive.nossalista.auth.repository.MagicLinkTokenRepository;
import br.com.leoferolive.nossalista.email.service.EmailService;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço para login por magic link.
 *
 * <p>Gera um token opaco de uso único (validade 10 min), dispara o e-mail via
 * {@link EmailService} (template {@code magic-link.html}) e, no consumo, valida o
 * token, marca o e-mail como verificado (posse comprovada) e devolve o usuário
 * para o controller emitir o JWT. Só loga contas já existentes — e-mail
 * inexistente é no-op silencioso (anti-enumeração).</p>
 */
@Service
public class MagicLinkService {

    private static final Logger log = LoggerFactory.getLogger(MagicLinkService.class);

    /** Validade do token. DEVE coincidir com SmtpEmailService.MAGIC_LINK_EXPIRATION_MINUTES. */
    private static final int MAGIC_LINK_EXPIRATION_MINUTES = 10;

    private final UserService userService;
    private final MagicLinkTokenRepository tokenRepository;
    private final EmailService emailService;

    public MagicLinkService(
        UserService userService,
        MagicLinkTokenRepository tokenRepository,
        EmailService emailService
    ) {
        this.userService = userService;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
    }

    /**
     * Solicita um magic link para o e-mail. No-op silencioso se a conta não existe
     * (anti-enumeração). Funciona para qualquer AuthProvider (inclui Google).
     */
    @Transactional
    public void requestMagicLink(String email) {
        String normalizedEmail = email.trim().toLowerCase();

        Optional<User> userOpt = userService.findByEmailOptional(normalizedEmail);
        if (userOpt.isEmpty()) {
            log.debug("Magic link requested for non-existent email: {}", normalizedEmail);
            return;
        }

        User user = userOpt.get();
        tokenRepository.deleteByUserIdAndUsedFalse(user.getId());

        String tokenValue = UUID.randomUUID().toString();
        MagicLinkToken token = new MagicLinkToken();
        token.setUserId(user.getId());
        token.setToken(tokenValue);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(MAGIC_LINK_EXPIRATION_MINUTES));
        tokenRepository.save(token);

        try {
            emailService.sendMagicLink(user.getEmail(), user.getName(), tokenValue);
        } catch (Exception e) {
            log.error("Failed to send magic link email to {}: {}", normalizedEmail, e.getMessage());
        }
    }

    /**
     * Valida e consome um token de magic link. Marca o e-mail como verificado e
     * retorna o usuário para o controller emitir o JWT.
     *
     * @throws InvalidMagicLinkTokenException se o token for inválido, expirado ou já usado
     */
    @Transactional
    public User consume(String token) {
        MagicLinkToken magicToken = tokenRepository.findByTokenAndUsedFalse(token)
            .orElseThrow(() -> new InvalidMagicLinkTokenException("Token inválido ou já utilizado"));

        if (magicToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidMagicLinkTokenException("Token expirado");
        }

        magicToken.setUsed(true);
        tokenRepository.save(magicToken);

        userService.markEmailVerified(magicToken.getUserId());

        return userService.findById(magicToken.getUserId())
            .orElseThrow(() -> new InvalidMagicLinkTokenException("Usuário não encontrado"));
    }
}
```

- [ ] **Step 4: Rodar o teste e confirmar que passa**

Run: `cd backend && ./mvnw -q test -Dtest=MagicLinkServiceTest`
Expected: PASS (todos os 7 testes verdes).

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/br/com/leoferolive/nossalista/auth/service/MagicLinkService.java \
        backend/src/test/java/br/com/leoferolive/nossalista/auth/service/MagicLinkServiceTest.java
git commit -m "feat(auth): MagicLinkService (solicitar e consumir magic link)"
```

---

## Task 3: Backend — DTOs, endpoints e handler de exceção (TDD)

**Files:**
- Create: `auth/dto/MagicLinkRequest.java`, `auth/dto/MagicLoginRequest.java`
- Modify: `config/GlobalExceptionHandler.java` (novo `@ExceptionHandler`)
- Modify: `auth/controller/AuthController.java` (injeção + 2 endpoints + constantes)
- Test: `auth/controller/MagicLinkControllerTest.java`

- [ ] **Step 1: Criar os DTOs**

`auth/dto/MagicLinkRequest.java`:

```java
package br.com.leoferolive.nossalista.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** DTO de requisição para solicitar um magic link. */
public record MagicLinkRequest(
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ter formato válido")
    String email
) {
}
```

`auth/dto/MagicLoginRequest.java`:

```java
package br.com.leoferolive.nossalista.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** DTO de requisição para consumir um magic link e autenticar. */
public record MagicLoginRequest(
    @NotBlank(message = "Token é obrigatório")
    String token
) {
}
```

- [ ] **Step 2: Escrever o teste de integração que falha**

`auth/controller/MagicLinkControllerTest.java` (espelha `AuthVerificationControllerTest`):

```java
package br.com.leoferolive.nossalista.auth.controller;

import br.com.leoferolive.nossalista.auth.domain.MagicLinkToken;
import br.com.leoferolive.nossalista.auth.repository.MagicLinkTokenRepository;
import br.com.leoferolive.nossalista.config.RateLimiterService;
import br.com.leoferolive.nossalista.support.RegressionTest;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.Role;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
@RegressionTest
class MagicLinkControllerTest {

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private UserRepository userRepository;
    @Autowired private MagicLinkTokenRepository tokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private RateLimiterService rateLimiterService;

    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
        rateLimiterService.reset();
    }

    private User persistUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setUsername("u" + UUID.randomUUID().toString().substring(0, 8));
        user.setPassword(passwordEncoder.encode("senha123"));
        user.setAuthProvider(AuthProvider.EMAIL);
        user.setRole(Role.USER);
        user.setEmailVerified(false);
        return userRepository.save(user);
    }

    @Test
    void requestMagicLinkAlwaysReturns200ForUnknownEmail() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("email", "nobody-" + UUID.randomUUID() + "@example.com");

        mockMvc.perform(post("/api/auth/magic-link")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
    }

    @Test
    void requestMagicLinkCreatesTokenForExistingUser() throws Exception {
        User user = persistUser("ana-" + UUID.randomUUID() + "@example.com");
        Map<String, String> request = new HashMap<>();
        request.put("email", user.getEmail());

        mockMvc.perform(post("/api/auth/magic-link")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        assertThat(tokenRepository.findAll())
            .anyMatch(t -> t.getUserId().equals(user.getId()) && !t.isUsed());
    }

    @Test
    void magicLoginWithValidTokenReturnsJwtAndVerifiesEmail() throws Exception {
        User user = persistUser("login-" + UUID.randomUUID() + "@example.com");
        MagicLinkToken token = new MagicLinkToken();
        token.setUserId(user.getId());
        token.setToken("valid-magic-token");
        token.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        tokenRepository.save(token);

        Map<String, String> request = new HashMap<>();
        request.put("token", "valid-magic-token");

        mockMvc.perform(post("/api/auth/magic-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.email").value(user.getEmail()));

        User refreshed = userRepository.findById(user.getId()).orElseThrow();
        assertThat(refreshed.isEmailVerified()).isTrue();
        assertThat(tokenRepository.findByTokenAndUsedFalse("valid-magic-token")).isEmpty();
    }

    @Test
    void magicLoginWithInvalidTokenReturns400() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("token", "bogus");

        mockMvc.perform(post("/api/auth/magic-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("https://api.nossalista.com/docs/errors/invalid-magic-link-token"))
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void requestMagicLinkReturns429AfterEmailRateLimit() throws Exception {
        User user = persistUser("rl-" + UUID.randomUUID() + "@example.com");
        Map<String, String> request = new HashMap<>();
        request.put("email", user.getEmail());
        String body = objectMapper.writeValueAsString(request);

        // Limite por e-mail = 5/1h: as 5 primeiras passam, a 6ª estoura.
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/magic-link")
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/auth/magic-link")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isTooManyRequests());
    }

    @Test
    void magicLoginReturns429AfterIpRateLimit() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("token", "bogus-" + UUID.randomUUID());
        String body = objectMapper.writeValueAsString(request);

        // Limite por IP = 10/15min: as 10 primeiras respondem 400 (token inválido),
        // a 11ª estoura o rate limit (o check ocorre antes do consume).
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/auth/magic-login")
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        }
        mockMvc.perform(post("/api/auth/magic-login")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isTooManyRequests());
    }
}
```

- [ ] **Step 3: Rodar e confirmar falha**

Run: `cd backend && ./mvnw -q test -Dtest=MagicLinkControllerTest`
Expected: FALHA — endpoints e handler ainda não existem (404/500 ou erro de compilação).

- [ ] **Step 4: Adicionar o handler de exceção**

Em `config/GlobalExceptionHandler.java`, adicionar o import
`import br.com.leoferolive.nossalista.auth.exception.InvalidMagicLinkTokenException;`
e, ao lado de `handleInvalidResetToken`, o handler:

```java
    /**
     * Trata exceção de token de magic link inválido, expirado ou já usado.
     * Retorna 400 Bad Request com RFC 7807 Problem Details.
     */
    @ExceptionHandler(InvalidMagicLinkTokenException.class)
    public ResponseEntity<ProblemDetail> handleInvalidMagicLinkToken(
        InvalidMagicLinkTokenException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.getMessage()
        );
        problem.setType(URI.create("https://api.nossalista.com/docs/errors/invalid-magic-link-token"));
        problem.setTitle("Token de magic link inválido");
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.badRequest().body(problem);
    }
```

- [ ] **Step 5: Adicionar endpoints + rate limit no `AuthController`**

Imports novos:
```java
import br.com.leoferolive.nossalista.auth.dto.MagicLinkRequest;
import br.com.leoferolive.nossalista.auth.dto.MagicLoginRequest;
import br.com.leoferolive.nossalista.auth.service.MagicLinkService;
import br.com.leoferolive.nossalista.user.domain.User;
```

Constantes (junto das demais no topo da classe):
```java
    private static final int MAGIC_LINK_LIMIT_PER_EMAIL = 5;
    private static final Duration MAGIC_LINK_WINDOW = Duration.ofHours(1);
    private static final int MAGIC_LINK_LIMIT_PER_IP = 15;
    private static final Duration MAGIC_LINK_IP_WINDOW = Duration.ofMinutes(15);
    private static final int MAGIC_LOGIN_LIMIT_PER_IP = 10;
    private static final Duration MAGIC_LOGIN_IP_WINDOW = Duration.ofMinutes(15);
```

Injetar `MagicLinkService` no construtor (adicionar o campo `private final MagicLinkService magicLinkService;`, o parâmetro no construtor e a atribuição — seguindo o padrão dos demais serviços já injetados).

Endpoints (após `resendVerification`):
```java
    /**
     * Solicita um magic link de login. Sempre retorna 200 (anti-enumeração).
     */
    @PostMapping("/magic-link")
    @Operation(
        summary = "Solicitar magic link de login",
        description = "Envia um link de acesso sem senha para o e-mail informado. Sempre retorna 200 para prevenir enumeração de e-mails."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Requisição processada (não confirma existência do e-mail)"),
        @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos"),
        @ApiResponse(responseCode = "429", description = "Muitas requisições — tente novamente mais tarde")
    })
    public ResponseEntity<Void> requestMagicLink(@Valid @RequestBody MagicLinkRequest request,
                                                 HttpServletRequest httpRequest) {
        String clientIp = clientIpResolver.resolve(httpRequest);
        String email = request.email().trim().toLowerCase();

        if (!rateLimiterService.isAllowed("magic-link:ip:" + clientIp,
                MAGIC_LINK_LIMIT_PER_IP, MAGIC_LINK_IP_WINDOW)) {
            throw new RateLimitExceededException("Muitas requisições. Tente novamente mais tarde.");
        }
        if (!rateLimiterService.isAllowed("magic-link:email:" + email,
                MAGIC_LINK_LIMIT_PER_EMAIL, MAGIC_LINK_WINDOW)) {
            throw new RateLimitExceededException("Muitas requisições para este e-mail. Tente novamente mais tarde.");
        }

        magicLinkService.requestMagicLink(request.email());
        return ResponseEntity.ok().build();
    }

    /**
     * Consome um magic link e autentica o usuário, retornando o JWT.
     */
    @PostMapping("/magic-login")
    @Operation(
        summary = "Login por magic link",
        description = "Consome o token do magic link (uso único, não expirado), marca o e-mail como verificado e retorna o JWT no formato do login."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token válido — retorna JWT e dados do usuário"),
        @ApiResponse(responseCode = "400", description = "Token inválido, expirado ou já utilizado"),
        @ApiResponse(responseCode = "429", description = "Muitas requisições — tente novamente mais tarde")
    })
    public ResponseEntity<LoginResponse> magicLogin(@Valid @RequestBody MagicLoginRequest request,
                                                    HttpServletRequest httpRequest) {
        String clientIp = clientIpResolver.resolve(httpRequest);

        if (!rateLimiterService.isAllowed("magic-login:ip:" + clientIp,
                MAGIC_LOGIN_LIMIT_PER_IP, MAGIC_LOGIN_IP_WINDOW)) {
            throw new RateLimitExceededException("Muitas tentativas. Tente novamente mais tarde.");
        }

        User user = magicLinkService.consume(request.token());
        String token = jwtService.generateToken(user);
        LoginResponse response = userMapper.toLoginResponse(user, token, jwtService.getExpirationTime());
        return ResponseEntity.ok(response);
    }
```

- [ ] **Step 6: Rodar e confirmar que passa**

Run: `cd backend && ./mvnw -q test -Dtest=MagicLinkControllerTest`
Expected: PASS (6 testes verdes, incluindo os dois de rate limit 429).

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/br/com/leoferolive/nossalista/auth/dto/MagicLinkRequest.java \
        backend/src/main/java/br/com/leoferolive/nossalista/auth/dto/MagicLoginRequest.java \
        backend/src/main/java/br/com/leoferolive/nossalista/config/GlobalExceptionHandler.java \
        backend/src/main/java/br/com/leoferolive/nossalista/auth/controller/AuthController.java \
        backend/src/test/java/br/com/leoferolive/nossalista/auth/controller/MagicLinkControllerTest.java
git commit -m "feat(auth): endpoints /magic-link e /magic-login com rate limit"
```

---

## Task 4: Frontend — métodos no `authApi`

**Files:**
- Modify: `frontend/src/api/authApi.ts`

- [ ] **Step 1: Adicionar os métodos ao objeto `authApi`**

Dentro do objeto `authApi` (após `resetPassword`), adicionar:

```ts
  /**
   * Solicita um magic link de login. O backend sempre responde 200
   * (anti-enumeração); a UI deve exibir sempre a mesma mensagem genérica.
   * @param email - Email da conta
   * @throws ApiError com status code em caso de falha (ex.: 429)
   */
  async requestMagicLink(email: string): Promise<void> {
    try {
      await client.post('/api/auth/magic-link', { email })
    } catch (error) {
      handleApiError(error)
    }
  },

  /**
   * Consome um magic link e autentica, retornando o JWT + dados do usuário.
   * @param token - Token recebido por e-mail
   * @returns Promise com os dados do usuário e o JWT (mesmo formato do OAuth exchange)
   * @throws ApiError com status code (ex.: 400 token inválido ou expirado)
   */
  async magicLogin(token: string): Promise<OAuthExchangeResponse> {
    try {
      const response = await client.post<OAuthExchangeResponse>('/api/auth/magic-login', { token })
      return response.data
    } catch (error) {
      handleApiError(error)
    }
  },
```

- [ ] **Step 2: Typecheck**

Run: `cd frontend && npm run typecheck`
Expected: sem erros.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/api/authApi.ts
git commit -m "feat(auth): metodos requestMagicLink e magicLogin no authApi"
```

---

## Task 5: Frontend — página `/magic-login` (TDD)

**Files:**
- Create: `frontend/src/pages/MagicLogin.tsx`
- Test: `frontend/src/pages/MagicLogin.test.tsx`
- Modify: `frontend/src/main.tsx` (rota + import)

> A página espelha `AuthCallback.tsx`: lê `?token=`, consome via `authApi.magicLogin`, persiste a sessão (`persistAuthToken` + `login()`) e redireciona para `/home`; em erro, mostra tela amigável com link para a landing. Sem a lógica de `pendingInviteCode` (fora do escopo — YAGNI).

- [ ] **Step 1: Escrever o teste que falha**

`frontend/src/pages/MagicLogin.test.tsx` — inspire-se em `pages/VerifyEmail.test.tsx` (padrão de mock do `authApi`, `MemoryRouter` com `initialEntries` contendo o `?token=`, e mock do `useAuth`). Casos:

```tsx
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { MagicLogin } from './MagicLogin'

const navigateMock = vi.fn()
const loginMock = vi.fn()
const magicLoginMock = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return { ...actual, useNavigate: () => navigateMock }
})
vi.mock('../contexts/AuthContext', () => ({ useAuth: () => ({ login: loginMock }) }))
vi.mock('../api/authApi', () => ({ authApi: { magicLogin: (t: string) => magicLoginMock(t) } }))

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/magic-login" element={<MagicLogin />} />
      </Routes>
    </MemoryRouter>
  )
}

describe('MagicLogin', () => {
  beforeEach(() => {
    navigateMock.mockReset()
    loginMock.mockReset()
    magicLoginMock.mockReset()
    localStorage.clear()
  })

  it('consome o token, loga e redireciona para /home', async () => {
    magicLoginMock.mockResolvedValue({
      id: '1', username: 'ana', email: 'ana@test.com', name: 'Ana',
      avatarUrl: null, onboardingCompletedAt: null, authProvider: 'EMAIL',
      createdAt: '2026-01-01', token: 'jwt-123', expiresAt: '2026-01-08',
    })

    renderAt('/magic-login?token=abc')

    await waitFor(() => expect(magicLoginMock).toHaveBeenCalledWith('abc'))
    await waitFor(() => expect(loginMock).toHaveBeenCalled())
    expect(navigateMock).toHaveBeenCalledWith('/home', { replace: true })
  })

  it('mostra erro quando o token está ausente', async () => {
    renderAt('/magic-login')
    await waitFor(() =>
      expect(screen.getByText(/link.*inválido|token.*não/i)).toBeInTheDocument()
    )
    expect(magicLoginMock).not.toHaveBeenCalled()
  })

  it('mostra erro quando o token é inválido/expirado', async () => {
    magicLoginMock.mockRejectedValue(new Error('Token inválido'))
    renderAt('/magic-login?token=bad')
    await waitFor(() => expect(screen.getByText(/não foi possível/i)).toBeInTheDocument())
  })
})
```

- [ ] **Step 2: Rodar e confirmar falha**

Run: `cd frontend && npm test -- --run src/pages/MagicLogin.test.tsx`
Expected: FALHA — `MagicLogin` não existe.

- [ ] **Step 3: Implementar `MagicLogin.tsx`**

`frontend/src/pages/MagicLogin.tsx`:

```tsx
import { useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { authApi } from '../api/authApi'
import { persistAuthToken } from '../auth/session'

export function MagicLogin() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { login } = useAuth()
  const [error, setError] = useState('')
  const hasProcessedRef = useRef(false)

  useEffect(() => {
    if (hasProcessedRef.current) {
      return
    }
    hasProcessedRef.current = true

    const token = searchParams.get('token')
    if (!token) {
      setError('Link inválido: token não encontrado.')
      return
    }

    const finish = async () => {
      try {
        const data = await authApi.magicLogin(token)
        persistAuthToken(data.token)
        login(data.token, {
          id: data.id,
          username: data.username,
          email: data.email,
          displayName: data.name,
          avatarUrl: data.avatarUrl ?? undefined,
          onboardingCompletedAt: data.onboardingCompletedAt ?? null,
        })
        navigate('/home', { replace: true })
      } catch (err) {
        setError(
          err instanceof Error
            ? 'Não foi possível entrar. O link pode ter expirado ou já ter sido usado.'
            : 'Não foi possível entrar com o link mágico.'
        )
      }
    }

    void finish()
  }, [searchParams, login, navigate])

  if (error) {
    return (
      <div className="nl-page flex items-center justify-center p-4">
        <div className="nl-card w-full max-w-md p-6 text-center">
          <h1 className="mb-2 font-display text-xl font-bold text-nl-text">Falha no Login</h1>
          <p className="mb-4 text-nl-muted">{error}</p>
          <button
            type="button"
            onClick={() => navigate('/', { replace: true })}
            className="nl-btn-primary w-full"
          >
            Voltar para o início
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="nl-page flex items-center justify-center p-4">
      <div className="nl-card w-full max-w-md p-6 text-center">
        <h1 className="mb-2 font-display text-xl font-bold text-nl-text">Entrando…</h1>
        <p className="text-nl-muted">Validando seu link de acesso.</p>
      </div>
    </div>
  )
}
```

- [ ] **Step 4: Registrar a rota em `main.tsx`**

Adicionar o import (junto dos outros de `./pages/...`):
```tsx
import { MagicLogin } from './pages/MagicLogin.tsx'
```
E a rota pública (ao lado de `/verify-email` / `/auth/callback`):
```tsx
      <Route path="/magic-login" element={<MagicLogin />} />
```

- [ ] **Step 5: Rodar e confirmar que passa**

Run: `cd frontend && npm test -- --run src/pages/MagicLogin.test.tsx`
Expected: PASS (3 testes).

- [ ] **Step 6: Commit**

```bash
git add frontend/src/pages/MagicLogin.tsx frontend/src/pages/MagicLogin.test.tsx frontend/src/main.tsx
git commit -m "feat(auth): pagina /magic-login que consome o token e loga"
```

---

## Task 6: Frontend — botão "Enviar link mágico" no `LoginModal` (TDD)

**Files:**
- Modify: `frontend/src/components/LoginModal.tsx`
- Test: `frontend/src/components/LoginModal.test.tsx`

> Ação **secundária** reusando o e-mail já digitado. "Entrar" continua o CTA primário. Mensagem genérica sempre (anti-enumeração), mesmo em erro leve.

- [ ] **Step 1: Escrever o teste que falha**

Adicionar em `LoginModal.test.tsx` (seguir o setup de mocks já existente no arquivo; se `authApi` ainda não está mockado lá, adicionar `vi.mock('../api/authApi', ...)` expondo `requestMagicLink`). Caso:

```tsx
it('envia magic link com o e-mail digitado e mostra mensagem genérica', async () => {
  // requestMagicLink mockado para resolver
  render(/* LoginModal dentro de MemoryRouter, como nos testes existentes */)
  await userEvent.type(screen.getByLabelText(/email/i), 'ana@test.com')
  await userEvent.click(screen.getByRole('button', { name: /link mágico/i }))
  await waitFor(() => expect(requestMagicLinkMock).toHaveBeenCalledWith('ana@test.com'))
  expect(screen.getByText(/enviamos um link de acesso/i)).toBeInTheDocument()
})

it('não chama a API se o e-mail estiver vazio', async () => {
  render(/* ... */)
  await userEvent.click(screen.getByRole('button', { name: /link mágico/i }))
  expect(requestMagicLinkMock).not.toHaveBeenCalled()
})
```

> Ajuste o `render(...)` ao helper já usado nos testes existentes do arquivo (mesmos providers/rota).

- [ ] **Step 2: Rodar e confirmar falha**

Run: `cd frontend && npm test -- --run src/components/LoginModal.test.tsx`
Expected: FALHA — botão/handler ainda não existem.

- [ ] **Step 3: Implementar o botão e o handler**

Em `LoginModal.tsx`:
1. Import: `import { authApi } from '../api/authApi'`.
2. Estados novos:
```tsx
  const [magicLoading, setMagicLoading] = useState(false)
  const [magicMessage, setMagicMessage] = useState('')
```
3. Handler:
```tsx
  const handleMagicLink = async () => {
    setError('')
    setMagicMessage('')
    if (!email.trim()) {
      setError('Informe seu e-mail para receber o link de acesso.')
      return
    }
    setMagicLoading(true)
    try {
      await authApi.requestMagicLink(email)
    } catch {
      // Anti-enumeração: a mensagem é sempre a mesma, mesmo em falha.
    } finally {
      setMagicMessage('Se existe uma conta com esse e-mail, enviamos um link de acesso.')
      setMagicLoading(false)
    }
  }
```
4. Mensagem de sucesso (acima do `<form>`, no padrão dos outros avisos):
```tsx
      {magicMessage && (
        <div className="mb-5 rounded-[1.2rem] border border-nl-primary/30 bg-nl-primary/10 px-4 py-3 text-sm text-nl-text">
          {magicMessage}
        </div>
      )}
```
5. Botão secundário logo após o botão "Entrar" (dentro do `<form>`, `type="button"` para não submeter o login por senha):
```tsx
        <button
          type="button"
          onClick={handleMagicLink}
          disabled={magicLoading}
          className="text-sm font-semibold text-nl-accent"
        >
          {magicLoading ? 'Enviando…' : 'Entrar com link mágico'}
        </button>
```

- [ ] **Step 4: Rodar e confirmar que passa**

Run: `cd frontend && npm test -- --run src/components/LoginModal.test.tsx`
Expected: PASS (novos testes verdes + os já existentes intactos).

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/LoginModal.tsx frontend/src/components/LoginModal.test.tsx
git commit -m "feat(auth): botao Enviar link magico no LoginModal"
```

---

## Task 7: Documentação (governança obrigatória)

**Files:**
- Modify: `docs/DECISIONS.md`, `docs/auth-endpoints-matrix.md`
- Revisar: `backend/.env.example`, `README.md`, `frontend/README.md`

- [ ] **Step 1: Registrar D-025 em `docs/DECISIONS.md`**

Adicionar, seguindo o formato das decisões existentes (`## D-025` + contexto/decisão/consequências), o registro do login por magic link: escopo login-only de contas existentes, consumo marca `email_verified`, token stateful UUID de 10 min (plaintext, consistente com reset/verificação), rate limits iguais ao reset de senha, UI só no `LoginModal`.

- [ ] **Step 2: Atualizar `docs/auth-endpoints-matrix.md`**

Ler o arquivo e adicionar duas linhas na matriz, no mesmo formato das existentes:
- `POST /api/auth/magic-link` — solicita magic link, 200 sempre (anti-enumeração), rate limit 5/email/1h + 15/IP/15min.
- `POST /api/auth/magic-login` — consome token, retorna JWT, rate limit 10/IP/15min, 400 se inválido/expirado.

- [ ] **Step 3: Revisar `.env.example` e READMEs**

Verificar se `backend/.env.example`, `README.md` ou `frontend/README.md` enumeram os fluxos de e-mail transacional / rotas públicas. Se sim, incluir o magic link (rota `/magic-login`). Se não listarem, nenhuma mudança é necessária — anotar no commit.

- [ ] **Step 4: Commit**

```bash
git add docs/DECISIONS.md docs/auth-endpoints-matrix.md
# + .env.example/READMEs se alterados
git commit -m "docs(auth): registra magic link (D-025) e atualiza matriz de endpoints"
```

---

## Task 8: Verificação final e PR

- [ ] **Step 1: Quality gate completo**

Run: `./scripts/quality.sh --pre-commit`
Expected: PASS (lint + typecheck + análise estática nos dois ecossistemas).

- [ ] **Step 2: Prettier (não coberto pelo pre-commit)**

Run: `cd frontend && npm run format:check`
Expected: sem arquivos fora do padrão. Se houver, rodar `npm run format` (ou `prettier --write`), revisar e commitar.

- [ ] **Step 3: Suíte de testes dos dois lados**

Run backend: `cd backend && ./mvnw -q test -Dtest='MagicLink*'`
Run frontend: `cd frontend && npm test -- --run src/pages/MagicLogin.test.tsx src/components/LoginModal.test.tsx`
Expected: tudo verde.

- [ ] **Step 4: Verificação manual do fluxo (opcional, recomendado)**

Com o backend em `dev` (`ConsoleEmailService` loga o token), solicitar um magic link e conferir no log o link `/magic-login?token=…`; abrir a rota no frontend `dev:mock` e confirmar o redirecionamento/estados. (A validação de UI usa o CLI `browser-use`, conforme as instruções do repositório.)

- [ ] **Step 5: Abrir o PR**

```bash
git push -u origin worktree-feat+magic-link-login
gh pr create --title "feat(auth): login por magic link" --body "$(cat <<'EOF'
Implementa o login por magic link ponta a ponta, conectando o código órfão
`EmailService.sendMagicLink`.

- Backend: tabela `magic_link_tokens` (V15), `MagicLinkService`, endpoints
  `POST /magic-link` (anti-enumeração) e `POST /magic-login` (emite JWT), com
  rate limiting espelhando o reset de senha. Consumir o link marca o e-mail
  como verificado.
- Frontend: botão secundário no `LoginModal` e página `/magic-login`.
- Docs: D-025 e matriz de endpoints de auth.

Spec: `docs/superpowers/specs/2026-07-03-magic-link-login-design.md`
Plano: `docs/superpowers/plans/2026-07-03-magic-link-login.md`

https://claude.ai/code/session_012rBahwcpkUjhK8WLcMSnec
EOF
)"
```

---

## Notas de execução

- **DRY / YAGNI / TDD**: cada task backend e frontend é test-first onde há lógica; a Task 1 é scaffolding de infra (sem teste isolado, no padrão do projeto).
- **Sincronia da expiração (10 min)**: `MagicLinkService.MAGIC_LINK_EXPIRATION_MINUTES` deve bater com `SmtpEmailService.MAGIC_LINK_EXPIRATION_MINUTES` (o template exibe "10 minutos"). Comentário cruzado já incluído no código.
- **Ordem**: Tasks 1→3 (backend) antes de 4→6 (frontend); 7 (docs) e 8 (verificação/PR) por último.
- **Quality gate**: rodar `./scripts/quality.sh --pre-commit` + `npm run format:check` antes de cada commit que toque código, não só no final.
