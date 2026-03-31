package br.com.leoferolive.nossalista.email.service;

/**
 * Exceção lançada quando o envio de e-mail falha.
 */
public class EmailSendException extends RuntimeException {

    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
