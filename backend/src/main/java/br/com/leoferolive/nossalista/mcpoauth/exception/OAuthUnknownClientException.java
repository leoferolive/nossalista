package br.com.leoferolive.nossalista.mcpoauth.exception;

/**
 * {@code client_id} desconhecido em {@code /oauth/authorize}. Nunca é seguro
 * redirecionar nesse caso (o {@code redirect_uri} do atacante não é confiável) —
 * tratada como 400 direto, sem redirect.
 */
public class OAuthUnknownClientException extends RuntimeException {

    public OAuthUnknownClientException(String clientId) {
        super("Unknown OAuth client_id: " + clientId);
    }
}
