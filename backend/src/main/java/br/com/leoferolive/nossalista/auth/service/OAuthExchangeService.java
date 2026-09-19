package br.com.leoferolive.nossalista.auth.service;

import br.com.leoferolive.nossalista.auth.exception.InvalidOAuthCodeException;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.service.UserService;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Orquestra a troca do one-time code OAuth2 (Q2.3) pelo JWT.
 *
 * <p>Consome o code single-use do {@link OAuthCodeStore}, reidrata o usuário a
 * partir do subject do JWT e devolve uma sessão autenticada. O controller
 * persiste o token apenas no cookie HttpOnly da resposta.</p>
 */
@Service
public class OAuthExchangeService {

    private final OAuthCodeStore oauthCodeStore;
    private final JwtService jwtService;
    private final UserService userService;

    public OAuthExchangeService(OAuthCodeStore oauthCodeStore, JwtService jwtService,
                                UserService userService) {
        this.oauthCodeStore = oauthCodeStore;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    /**
     * Troca um one-time code pelo JWT, sem expô-lo ao browser.
     *
     * @param code one-time code recebido do frontend
     * @return sessão autenticada para emissão do cookie HttpOnly
     * @throws InvalidOAuthCodeException se o code for inválido, expirado ou já consumido
     */
    public AuthenticatedSession exchange(String code) {
        OAuthCodeStore.OAuthCodeClaim claim = oauthCodeStore.consumeForExchange(code)
            .orElseThrow(() -> new InvalidOAuthCodeException("Código de login inválido ou expirado"));

        if (claim.userId() != null) {
            return issueFreshSession(claim.userId());
        }

        return issueLegacySession(claim.legacyJwt());
    }

    private AuthenticatedSession issueFreshSession(UUID userId) {
        User user = userService.findById(userId)
            .orElseThrow(() -> new InvalidOAuthCodeException("Código de login inválido ou expirado"));
        return new AuthenticatedSession(user, jwtService.generateToken(user));
    }

    private AuthenticatedSession issueLegacySession(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidOAuthCodeException("Código de login inválido ou expirado");
        }
        UUID userId;
        try {
            userId = jwtService.extractUserId(token);
        } catch (RuntimeException exception) {
            throw new InvalidOAuthCodeException("Código de login inválido ou expirado");
        }
        User user = userService.findById(userId)
            .orElseThrow(() -> new InvalidOAuthCodeException("Código de login inválido ou expirado"));

        return new AuthenticatedSession(user, token);
    }
}
