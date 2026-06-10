package br.com.leoferolive.nossalista.auth.controller;

import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.auth.dto.ForgotPasswordRequest;
import br.com.leoferolive.nossalista.auth.dto.LoginRequest;
import br.com.leoferolive.nossalista.auth.dto.LoginResponse;
import br.com.leoferolive.nossalista.auth.dto.OAuthExchangeRequest;
import br.com.leoferolive.nossalista.auth.dto.RegisterRequest;
import br.com.leoferolive.nossalista.auth.dto.RegisterResponse;
import br.com.leoferolive.nossalista.auth.dto.ResendVerificationRequest;
import br.com.leoferolive.nossalista.auth.dto.ResetPasswordRequest;
import br.com.leoferolive.nossalista.auth.dto.UserMapper;
import br.com.leoferolive.nossalista.auth.service.AuthService;
import br.com.leoferolive.nossalista.auth.service.EmailVerificationService;
import br.com.leoferolive.nossalista.auth.service.JwtService;
import br.com.leoferolive.nossalista.auth.service.OAuthExchangeService;
import br.com.leoferolive.nossalista.auth.service.PasswordResetService;
import br.com.leoferolive.nossalista.common.exception.RateLimitExceededException;
import br.com.leoferolive.nossalista.config.RateLimiterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Controller REST para operações de autenticação
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Endpoints de autenticação e registro de usuários")
public class AuthController {

    private static final int FORGOT_PASSWORD_LIMIT_PER_EMAIL = 5;
    private static final Duration FORGOT_PASSWORD_WINDOW = Duration.ofHours(1);
    private static final int FORGOT_PASSWORD_LIMIT_PER_IP = 15;
    private static final Duration FORGOT_PASSWORD_IP_WINDOW = Duration.ofMinutes(15);
    private static final int RESET_PASSWORD_LIMIT_PER_IP = 10;
    private static final Duration RESET_PASSWORD_IP_WINDOW = Duration.ofMinutes(15);
    private static final int RESEND_VERIFICATION_LIMIT_PER_EMAIL = 3;
    private static final Duration RESEND_VERIFICATION_WINDOW = Duration.ofHours(1);
    private static final int RESEND_VERIFICATION_LIMIT_PER_IP = 10;
    private static final Duration RESEND_VERIFICATION_IP_WINDOW = Duration.ofMinutes(15);

    private final AuthService authService;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final PasswordResetService passwordResetService;
    private final RateLimiterService rateLimiterService;
    private final OAuthExchangeService oauthExchangeService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(AuthService authService, JwtService jwtService,
                          UserMapper userMapper, PasswordResetService passwordResetService,
                          RateLimiterService rateLimiterService,
                          OAuthExchangeService oauthExchangeService,
                          EmailVerificationService emailVerificationService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.passwordResetService = passwordResetService;
        this.rateLimiterService = rateLimiterService;
        this.oauthExchangeService = oauthExchangeService;
        this.emailVerificationService = emailVerificationService;
    }

    /**
     * Registra um novo usuário com email e senha
     *
     * @param request dados de registro com validação
     * @return usuário criado sem password
     */
    @PostMapping("/register")
    @Operation(
        summary = "Registrar novo usuário",
        description = "Cria uma nova conta de usuário com email, username e senha. Senha é hasheada com BCrypt."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos (erros de validação)"),
        @ApiResponse(responseCode = "409", description = "Email ou username já existe")
    })
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);
        RegisterResponse response = userMapper.toRegisterResponse(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Faz login de um usuário com email e senha
     *
     * @param request credenciais de login (email e senha)
     * @return dados do usuário autenticado com JWT token
     */
    @PostMapping("/login")
    @Operation(
        summary = "Fazer login",
        description = "Autentica usuário com email e senha, retornando JWT token com validade de 7 dias."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login bem-sucedido com JWT token"),
        @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos (campos obrigatórios vazios)"),
        @ApiResponse(responseCode = "401", description = "Credenciais inválidas (email não existe OU senha incorreta)")
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        // Validar credenciais
        User user = authService.login(request);

        // Gerar JWT token
        String token = jwtService.generateToken(user);
        LocalDateTime expiresAt = jwtService.getExpirationTime();

        // Criar response com dados do usuário + token
        LoginResponse response = userMapper.toLoginResponse(user, token, expiresAt);

        return ResponseEntity.ok(response);
    }

    /**
     * Inicia fluxo de autenticação OAuth2 com Google
     * <p>
     * Redireciona para o endpoint do Spring Security que inicia o fluxo OAuth2.
     * Spring Security automaticamente redireciona para o Google consent screen.
     *
     * @param response resposta HTTP para fazer redirect
     * @throws IOException se houver erro no redirect
     */
    @GetMapping("/google")
    @Operation(
        summary = "Iniciar login com Google OAuth2",
        description = "Inicia o fluxo de autenticação OAuth2 com Google. Redireciona para consent screen do Google."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "302", description = "Redirect para Google consent screen"),
        @ApiResponse(responseCode = "500", description = "Erro ao iniciar fluxo OAuth2")
    })
    public void initiateGoogleLogin(HttpServletResponse response) throws IOException {
        // Spring Security intercepta /oauth2/authorization/google e inicia fluxo OAuth2
        response.sendRedirect("/oauth2/authorization/google");
    }

    /**
     * Troca o one-time code do OAuth2 (Q2.3) pelo JWT.
     * <p>
     * O fluxo OAuth2 não coloca mais o JWT na URL — emite um code opaco de uso
     * único. Este endpoint consome o code (single-use, não-expirado) e retorna o
     * JWT no corpo, no mesmo formato do login normal.
     *
     * @param request corpo com o one-time code
     * @return dados do usuário autenticado com JWT token
     */
    @PostMapping("/oauth/exchange")
    @Operation(
        summary = "Trocar one-time code OAuth2 por JWT",
        description = "Consome o code opaco de uso único gerado no sucesso do OAuth2 e retorna o JWT. "
            + "O code expira em 60s e só pode ser trocado uma vez."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Code válido — retorna JWT e dados do usuário"),
        @ApiResponse(responseCode = "400", description = "Code inválido, expirado ou já utilizado")
    })
    public ResponseEntity<LoginResponse> exchangeOAuthCode(@Valid @RequestBody OAuthExchangeRequest request) {
        LoginResponse response = oauthExchangeService.exchange(request.code());
        return ResponseEntity.ok(response);
    }

    /**
     * Solicita reset de senha via email.
     * Sempre retorna 200 para prevenir enumeração de emails.
     *
     * @param request dados com email do usuário
     * @return 200 OK sempre (independente de email existir ou não)
     */
    @PostMapping("/forgot-password")
    @Operation(
        summary = "Solicitar reset de senha",
        description = "Envia link de reset de senha para o email informado. Sempre retorna 200 para prevenir enumeração de emails."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Requisição processada (não confirma existência do email)"),
        @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos"),
        @ApiResponse(responseCode = "429", description = "Muitas requisições — tente novamente mais tarde")
    })
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                               HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        String email = request.email().trim().toLowerCase();

        if (!rateLimiterService.isAllowed("forgot-password:ip:" + clientIp,
                FORGOT_PASSWORD_LIMIT_PER_IP, FORGOT_PASSWORD_IP_WINDOW)) {
            throw new RateLimitExceededException("Muitas requisições. Tente novamente mais tarde.");
        }
        if (!rateLimiterService.isAllowed("forgot-password:email:" + email,
                FORGOT_PASSWORD_LIMIT_PER_EMAIL, FORGOT_PASSWORD_WINDOW)) {
            throw new RateLimitExceededException("Muitas requisições para este email. Tente novamente mais tarde.");
        }

        passwordResetService.requestReset(request.email());
        return ResponseEntity.ok().build();
    }

    /**
     * Redefine a senha usando um token válido
     *
     * @param request dados com token e nova senha
     * @return 200 OK se senha redefinida com sucesso
     */
    @PostMapping("/reset-password")
    @Operation(
        summary = "Redefinir senha",
        description = "Redefine a senha do usuário usando um token de reset válido e não expirado."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Senha redefinida com sucesso"),
        @ApiResponse(responseCode = "400", description = "Token inválido, expirado ou dados inválidos"),
        @ApiResponse(responseCode = "429", description = "Muitas requisições — tente novamente mais tarde")
    })
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                                              HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);

        if (!rateLimiterService.isAllowed("reset-password:ip:" + clientIp,
                RESET_PASSWORD_LIMIT_PER_IP, RESET_PASSWORD_IP_WINDOW)) {
            throw new RateLimitExceededException("Muitas tentativas. Tente novamente mais tarde.");
        }

        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok().build();
    }

    /**
     * Verifica o e-mail de um usuário usando um token válido (Q2.7).
     *
     * @param token token de verificação recebido por e-mail
     * @return 200 OK se o e-mail foi verificado com sucesso
     */
    @GetMapping("/verify-email")
    @Operation(
        summary = "Verificar e-mail",
        description = "Valida o token de verificação enviado por e-mail, marca o e-mail como verificado e consome o token."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "E-mail verificado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Token inválido, expirado ou já utilizado")
    })
    public ResponseEntity<Void> verifyEmail(@RequestParam("token") String token) {
        emailVerificationService.verify(token);
        return ResponseEntity.ok().build();
    }

    /**
     * Reenvia o e-mail de verificação. Sempre retorna 200 para prevenir
     * enumeração de e-mails. Aplica rate limiting por e-mail e por IP (Q2.7).
     *
     * @param request dados com o e-mail do usuário
     * @return 200 OK sempre (independente de e-mail existir/estar verificado)
     */
    @PostMapping("/resend-verification")
    @Operation(
        summary = "Reenviar e-mail de verificação",
        description = "Reenvia o e-mail de verificação. Sempre retorna 200 para prevenir enumeração de e-mails."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Requisição processada (não confirma existência do e-mail)"),
        @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos"),
        @ApiResponse(responseCode = "429", description = "Muitas requisições — tente novamente mais tarde")
    })
    public ResponseEntity<Void> resendVerification(@Valid @RequestBody ResendVerificationRequest request,
                                                   HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        String email = request.email().trim().toLowerCase();

        if (!rateLimiterService.isAllowed("resend-verification:ip:" + clientIp,
                RESEND_VERIFICATION_LIMIT_PER_IP, RESEND_VERIFICATION_IP_WINDOW)) {
            throw new RateLimitExceededException("Muitas requisições. Tente novamente mais tarde.");
        }
        if (!rateLimiterService.isAllowed("resend-verification:email:" + email,
                RESEND_VERIFICATION_LIMIT_PER_EMAIL, RESEND_VERIFICATION_WINDOW)) {
            throw new RateLimitExceededException("Muitas requisições para este e-mail. Tente novamente mais tarde.");
        }

        emailVerificationService.resendVerification(request.email());
        return ResponseEntity.ok().build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
