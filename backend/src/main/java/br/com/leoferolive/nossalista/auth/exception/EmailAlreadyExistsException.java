package br.com.leoferolive.nossalista.auth.exception;

/**
 * Exceção lançada ao tentar registrar com um email que já existe
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("Email '" + email + "' já está em uso");
    }
}
