package br.com.leoferolive.nossalista.auth.exception;

/**
 * Exceção lançada quando um token de verificação de e-mail é inválido,
 * expirado ou já foi usado (Q2.7).
 */
public class InvalidVerificationTokenException extends RuntimeException {

    public InvalidVerificationTokenException(String message) {
        super(message);
    }
}
