package br.com.leoferolive.nossalista.auth;

import br.com.leoferolive.nossalista.auth.provider.GoogleOAuth2ClaimsAdapter;
import br.com.leoferolive.nossalista.auth.service.GoogleIdentityRejectedException;
import br.com.leoferolive.nossalista.auth.service.GoogleIdentityService;
import br.com.leoferolive.nossalista.auth.service.OAuthCodeStore;
import br.com.leoferolive.nossalista.user.domain.User;
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

    private final GoogleIdentityService googleIdentityService;
    private final OAuthCodeStore oauthCodeStore;
    private final GoogleOAuth2ClaimsAdapter googleClaimsAdapter;

    @Value("${frontend.url:http://localhost:5173}")
    private String frontendUrl;

    /**
     * Configures the Google callback handler with its typed provider adapter.
     *
     * <pre>{@code
     * new OAuth2SuccessHandler(identityService, oauthCodeStore, googleClaimsAdapter);
     * }</pre>
     *
     * @param googleIdentityService resolver for trusted Google identities
     * @param oauthCodeStore issuer for one-time browser handoff codes
     * @param googleClaimsAdapter adapter for Google's OAuth2 user payload
     */
    public OAuth2SuccessHandler(GoogleIdentityService googleIdentityService,
                                OAuthCodeStore oauthCodeStore,
                                GoogleOAuth2ClaimsAdapter googleClaimsAdapter) {
        this.googleIdentityService = googleIdentityService;
        this.oauthCodeStore = oauthCodeStore;
        this.googleClaimsAdapter = googleClaimsAdapter;
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
        try {
            User user = googleIdentityService.resolve(googleClaimsAdapter.from(oauth2User));
            String code = oauthCodeStore.issue(user);
            response.sendRedirect(String.format("%s/auth/callback?code=%s", frontendUrl, code));
        } catch (GoogleIdentityRejectedException exception) {
            response.sendRedirect(frontendUrl + "/auth/callback?error=google_identity_rejected");
        }
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
