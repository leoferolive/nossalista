package br.com.leoferolive.nossalista.common.exception;

/**
 * Exceção lançada quando o rate limit é excedido.
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
