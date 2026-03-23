package br.com.leoferolive.nossalista.common.exception;

/**
 * Exceção lançada quando o input do usuário é inválido (validação manual)
 */
public class InvalidInputException extends RuntimeException {

    public InvalidInputException(String message) {
        super(message);
    }
}
