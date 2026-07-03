package br.com.leoferolive.nossalista.mcpoauth.exception;

/**
 * {@code redirect_uri} não registrado para o cliente em {@code /oauth/authorize}.
 * Nunca é seguro redirecionar para uma URI não confiável — tratada como 400
 * direto, sem redirect (evita open redirect).
 */
public class OAuthInvalidRedirectUriException extends RuntimeException {

    public OAuthInvalidRedirectUriException(String clientId, String redirectUri) {
        super("redirect_uri not registered for client '" + clientId + "': " + redirectUri);
    }
}
