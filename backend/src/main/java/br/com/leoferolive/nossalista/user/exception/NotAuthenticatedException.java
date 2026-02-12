package br.com.leoferolive.nossalista.user.exception;

/**
 * Exceção lançada quando usuário tenta acessar endpoint sem autenticação
 */
public class NotAuthenticatedException extends RuntimeException {

    public NotAuthenticatedException(String message) {
        super(message);
    }
}
