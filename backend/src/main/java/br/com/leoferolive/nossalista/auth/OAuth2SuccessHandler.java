package br.com.leoferolive.nossalista.auth;

import br.com.leoferolive.nossalista.auth.service.AuthService;
import br.com.leoferolive.nossalista.auth.service.JwtService;
import br.com.leoferolive.nossalista.auth.service.OAuthCodeStore;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.Role;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Map;

/**
 * Handler para processar sucesso de autenticação OAuth2 (Google)
 * <p>
 * Responsável por:
 * - Extrair dados do usuário do Google (email, name, picture)
 * - Criar novo usuário se não existir
 * - Atualizar informações se usuário já existir
 * - Gerar JWT token
 * - Emitir um one-time code (Q2.3) e redirecionar para o frontend com {@code ?code=}
 *   (nunca o JWT na URL, para não vazar em histórico/logs/Referer)
 */
@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthService authService;
    private final OAuthCodeStore oauthCodeStore;

    @Value("${frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public OAuth2SuccessHandler(UserRepository userRepository, JwtService jwtService,
                                AuthService authService, OAuthCodeStore oauthCodeStore) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.authService = authService;
        this.oauthCodeStore = oauthCodeStore;
    }

    /**
     * Processa autenticação bem-sucedida do Google OAuth2
     *
     * @param request requisição HTTP
     * @param response resposta HTTP
     * @param authentication objeto de autenticação OAuth2
     * @throws IOException se houver erro no redirect
     */
    @Transactional
    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException {

        OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = oauth2Token.getPrincipal();
        Map<String, Object> attributes = oauth2User.getAttributes();

        // Extrair dados do Google
        String rawEmail = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");

        // Validar email (obrigatório)
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new IllegalStateException("Email não fornecido pelo Google");
        }

        // Normalizar email (trim + toLowerCase)
        final String email = rawEmail.trim().toLowerCase();

        // Buscar ou criar usuário. createGoogleUser já persiste com todos os
        // campos corretos (incl. emailVerified=true), então a reconciliação
        // abaixo só se aplica a contas pré-existentes para não salvar duas vezes.
        User existing = userRepository.findByEmail(email).orElse(null);
        User user = existing != null ? existing : createGoogleUser(email, name, picture);

        // Atualizar informações se mudaram (apenas usuários GOOGLE pré-existentes)
        if (existing != null && user.getAuthProvider() == AuthProvider.GOOGLE) {
            reconcileExistingGoogleUser(user, name, picture);
        }

        // Gerar JWT token
        String token = jwtService.generateToken(user);

        // Q2.3: NÃO colocar o JWT na URL. Emite um one-time code e redireciona o
        // frontend apenas com ?code=, que será trocado pelo JWT via POST seguro.
        String code = oauthCodeStore.issue(token);
        String redirectUrl = String.format("%s/auth/callback?code=%s", frontendUrl, code);
        response.sendRedirect(redirectUrl);
    }

    /**
     * Reconcilia dados de um usuário GOOGLE pré-existente com o que o provedor
     * informou agora (nome, avatar) e garante {@code emailVerified=true}. Só
     * persiste se algo mudou, evitando writes redundantes.
     *
     * @param user    usuário pré-existente (provider GOOGLE)
     * @param name    nome atual vindo do Google
     * @param picture avatar atual vindo do Google
     */
    private void reconcileExistingGoogleUser(User user, String name, String picture) {
        boolean updated = false;

        if (name != null && !name.equals(user.getName())) {
            user.setName(name);
            updated = true;
        }

        if (picture != null && !picture.equals(user.getAvatarUrl())) {
            user.setAvatarUrl(picture);
            updated = true;
        }

        // O Google já verificou o e-mail; garante o flag para contas pré-existentes.
        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            updated = true;
        }

        if (updated) {
            userRepository.save(user);
        }
    }

    /**
     * Cria novo usuário a partir de dados do Google OAuth2
     *
     * @param email email do Google
     * @param name nome completo do Google
     * @param picture URL da foto de perfil do Google
     * @return usuário criado e salvo
     */
    private User createGoogleUser(String email, String name, String picture) {
        String username = authService.generateUniqueUsername(email);

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setUsername(username);
        newUser.setName(name);
        newUser.setAvatarUrl(picture);
        newUser.setAuthProvider(AuthProvider.GOOGLE);
        newUser.setPassword(null); // OAuth2 não tem senha
        newUser.setRole(Role.USER);
        newUser.setEmailVerified(true); // Google já verifica o e-mail do usuário

        return userRepository.save(newUser);
    }

    /**
     * Setter para injeção de frontendUrl (usado em testes)
     *
     * @param frontendUrl URL do frontend
     */
    public void setFrontendUrl(String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }
}
