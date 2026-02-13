package br.com.leoferolive.nossalista.listitem.exception;

/**
 * Exceção lançada quando um item não é encontrado no banco de dados
 */
public class ItemNotFoundException extends RuntimeException {

    public ItemNotFoundException(String message) {
        super(message);
    }
}
