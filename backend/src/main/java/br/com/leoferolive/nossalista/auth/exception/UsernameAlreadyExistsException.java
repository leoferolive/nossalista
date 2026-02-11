package br.com.leoferolive.nossalista.auth.exception;

/**
 * Exceção lançada ao tentar registrar com um username que já existe
 */
public class UsernameAlreadyExistsException extends RuntimeException {

    public UsernameAlreadyExistsException(String username) {
        super("Username '" + username + "' já está em uso");
    }
}
