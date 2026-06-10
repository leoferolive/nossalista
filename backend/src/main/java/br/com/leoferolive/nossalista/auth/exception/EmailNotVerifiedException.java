package br.com.leoferolive.nossalista.auth.exception;

/**
 * Exceção lançada quando o login email/senha é bloqueado por e-mail não
 * verificado e o enforcement estrito está ligado
 * ({@code app.auth.require-email-verification=true}) (Q2.7).
 */
public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
