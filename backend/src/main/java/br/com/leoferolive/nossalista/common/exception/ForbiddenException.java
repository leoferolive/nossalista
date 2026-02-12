package br.com.leoferolive.nossalista.common.exception;

/**
 * Exceção lançada quando o usuário não tem permissão para acessar um recurso
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
