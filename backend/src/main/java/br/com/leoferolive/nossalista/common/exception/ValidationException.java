package br.com.leoferolive.nossalista.common.exception;

/**
 * Exceção lançada quando há erro de validação de regras de negócio
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
