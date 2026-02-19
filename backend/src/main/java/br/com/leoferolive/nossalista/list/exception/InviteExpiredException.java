package br.com.leoferolive.nossalista.list.exception;

/**
 * Exceção lançada quando um link de convite expirou
 */
public class InviteExpiredException extends RuntimeException {

    public InviteExpiredException(String message) {
        super(message);
    }
}
