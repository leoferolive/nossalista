package br.com.leoferolive.nossalista.auth.exception;

/**
 * Exception thrown when attempting to register with an email that already exists
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("Email '" + email + "' já está em uso");
    }
}
