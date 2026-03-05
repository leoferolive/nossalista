package br.com.leoferolive.nossalista.auth.controller;

import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.auth.dto.LoginRequest;
import br.com.leoferolive.nossalista.auth.dto.LoginResponse;
import br.com.leoferolive.nossalista.auth.dto.RegisterRequest;
import br.com.leoferolive.nossalista.auth.dto.RegisterResponse;
import br.com.leoferolive.nossalista.auth.dto.UserMapper;
import br.com.leoferolive.nossalista.auth.service.AuthService;
import br.com.leoferolive.nossalista.auth.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Controller REST para operações de autenticação
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Endpoints de autenticação e registro de usuários")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public AuthController(AuthService authService, JwtService jwtService, UserMapper userMapper) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
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
}
