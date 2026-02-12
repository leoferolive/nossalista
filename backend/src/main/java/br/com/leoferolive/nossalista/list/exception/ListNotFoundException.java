package br.com.leoferolive.nossalista.list.exception;

/**
 * Exceção lançada quando uma lista não é encontrada no banco de dados
 */
public class ListNotFoundException extends RuntimeException {

    public ListNotFoundException(String message) {
        super(message);
    }
}
