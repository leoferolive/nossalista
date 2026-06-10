package br.com.leoferolive.nossalista.auth.exception;

/**
 * Exceção lançada quando um one-time code de login OAuth2 é inválido,
 * expirado ou já foi consumido (Q2.3).
 */
public class InvalidOAuthCodeException extends RuntimeException {

    public InvalidOAuthCodeException(String message) {
        super(message);
    }
}
