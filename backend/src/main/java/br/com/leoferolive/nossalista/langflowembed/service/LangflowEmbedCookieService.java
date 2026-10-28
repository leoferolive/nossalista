package br.com.leoferolive.nossalista.langflowembed.service;

import br.com.leoferolive.nossalista.langflowembed.config.LangflowEmbedProperties;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Monta o cookie que leva o token do embed até o Langflow, para o launch e para o logout.
 *
 * <p>Os atributos são os mesmos nos dois casos (nome, path, {@code Secure}, {@code SameSite}),
 * para que o navegador trate a expiração como o mesmo cookie. Sem {@code Domain}, para o
 * cookie ficar restrito ao host que emitiu a sessão.</p>
 */
@Service
public class LangflowEmbedCookieService {

    private final LangflowEmbedProperties properties;

    public LangflowEmbedCookieService(LangflowEmbedProperties properties) {
        this.properties = properties;
    }

    /**
     * Exemplo: {@code ResponseCookie cookie = cookies.launchCookie(token);}
     *
     * @param token JWT emitido para o usuário
     * @return cookie com {@code Max-Age} igual ao TTL do token
     */
    public ResponseCookie launchCookie(String token) {
        return cookie(token, properties.getTokenTtl());
    }

    /**
     * Expira o cookie do Langflow no logout. Com o embed desligado nenhum cookie foi
     * gravado, então nada é escrito.
     *
     * @param response resposta do logout, onde o {@code Set-Cookie} de expiração é anexado
     */
    public void expireLaunchCookie(HttpServletResponse response) {
        if (!properties.isEnabled()) {
            return;
        }
        response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString());
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(properties.getCookieName(), value)
            .path(properties.getCookiePath())
            .httpOnly(true)
            .secure(properties.isCookieSecure())
            .sameSite("Lax")
            .maxAge(maxAge)
            .build();
    }
}
