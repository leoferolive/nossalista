package br.com.leoferolive.nossalista.auth.exception;

/**
 * Exceção lançada quando um token de reset de senha é inválido, expirado ou já usado
 */
public class InvalidResetTokenException extends RuntimeException {

    public InvalidResetTokenException(String message) {
        super(message);
    }
}
