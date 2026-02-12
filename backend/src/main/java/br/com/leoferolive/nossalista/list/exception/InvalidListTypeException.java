package br.com.leoferolive.nossalista.list.exception;

/**
 * Exceção lançada quando um tipo de lista inválido é fornecido
 *
 * Exemplo: typeId = 999 (válidos: 1-4)
 */
public class InvalidListTypeException extends RuntimeException {

    private final Integer invalidTypeId;

    public InvalidListTypeException(Integer invalidTypeId) {
        super(String.format(
            "Tipo de lista inválido: %d. Tipos válidos: 1-4 (Compras, Tarefas, Wishlist, Genérica)",
            invalidTypeId
        ));
        this.invalidTypeId = invalidTypeId;
    }

    public Integer getInvalidTypeId() {
        return invalidTypeId;
    }
}
