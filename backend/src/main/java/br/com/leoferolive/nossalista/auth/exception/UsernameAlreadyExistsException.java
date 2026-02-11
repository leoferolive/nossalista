package br.com.leoferolive.nossalista.auth.exception;

/**
 * Exception thrown when attempting to register with a username that already exists
 */
public class UsernameAlreadyExistsException extends RuntimeException {

    public UsernameAlreadyExistsException(String username) {
        super("Username '" + username + "' já está em uso");
    }
}
