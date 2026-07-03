package br.com.leoferolive.nossalista.mcpoauth.exception;

/**
 * O pedido de consentimento não pode ser visto/decidido por esta requisição —
 * cookie de vínculo ausente/incorreto, ou usuário diferente do que reivindicou
 * o pedido (ver {@code PendingAuthorization}, defesa contra sequestro de
 * consentimento cross-user). Sempre 403 — nunca 404, para não confundir com
 * "pedido inexistente" (esses casos são tratados por
 * {@link PendingAuthorizationNotFoundException}).
 */
public class OAuthConsentForbiddenException extends RuntimeException {

    public OAuthConsentForbiddenException(String message) {
        super(message);
    }
}
