package br.com.leoferolive.nossalista.auth.service;

import br.com.leoferolive.nossalista.auth.dto.LoginResponse;
import br.com.leoferolive.nossalista.auth.dto.UserMapper;
import br.com.leoferolive.nossalista.auth.exception.InvalidOAuthCodeException;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.service.UserService;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Orquestra a troca do one-time code OAuth2 (Q2.3) pelo JWT.
 *
 * <p>Consome o code single-use do {@link OAuthCodeStore}, reidrata o usuário a
 * partir do subject do JWT e devolve um {@link LoginResponse} no mesmo formato
 * do login normal — para que o frontend persista o token no localStorage como
 * já faz hoje.</p>
 */
@Service
public class OAuthExchangeService {

    private final OAuthCodeStore oauthCodeStore;
    private final JwtService jwtService;
    private final UserService userService;
    private final UserMapper userMapper;

    public OAuthExchangeService(OAuthCodeStore oauthCodeStore, JwtService jwtService,
                                UserService userService, UserMapper userMapper) {
        this.oauthCodeStore = oauthCodeStore;
        this.jwtService = jwtService;
        this.userService = userService;
        this.userMapper = userMapper;
    }

    /**
     * Troca um one-time code pelo JWT, devolvendo a resposta de login.
     *
     * @param code one-time code recebido do frontend
     * @return resposta de login com o JWT e dados do usuário
     * @throws InvalidOAuthCodeException se o code for inválido, expirado ou já consumido
     */
    public LoginResponse exchange(String code) {
        String token = oauthCodeStore.consume(code)
            .orElseThrow(() -> new InvalidOAuthCodeException("Código de login inválido ou expirado"));

        UUID userId = jwtService.extractUserId(token);
        User user = userService.findById(userId)
            .orElseThrow(() -> new InvalidOAuthCodeException("Código de login inválido ou expirado"));

        return userMapper.toLoginResponse(user, token, jwtService.getExpirationTime());
    }
}
