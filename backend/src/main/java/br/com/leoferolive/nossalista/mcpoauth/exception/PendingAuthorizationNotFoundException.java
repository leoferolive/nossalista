package br.com.leoferolive.nossalista.mcpoauth.exception;

/**
 * Pedido de autorização pendente (tela de consentimento) inexistente, expirado
 * ou já decidido (aprovado/negado). Retornada pelos endpoints de consentimento
 * ({@code /api/oauth/consent/**}) como 404.
 */
public class PendingAuthorizationNotFoundException extends RuntimeException {

    public PendingAuthorizationNotFoundException(String message) {
        super(message);
    }
}
