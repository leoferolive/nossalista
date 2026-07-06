package br.com.leoferolive.nossalista.auth.exception;

/**
 * Exceção lançada quando um token de magic link é inválido, expirado ou já usado.
 */
public class InvalidMagicLinkTokenException extends RuntimeException {

    public InvalidMagicLinkTokenException(String message) {
        super(message);
    }
}
