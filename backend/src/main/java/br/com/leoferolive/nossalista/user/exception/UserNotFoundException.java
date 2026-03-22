package br.com.leoferolive.nossalista.user.exception;

/**
 * Exceção lançada quando um usuário não é encontrado no sistema
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
