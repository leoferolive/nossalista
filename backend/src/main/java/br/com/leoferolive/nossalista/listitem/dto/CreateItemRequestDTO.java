package br.com.leoferolive.nossalista.listitem.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

/**
 * DTO de requisição para criar um novo item na lista
 *
 * Campos dinâmicos por tipo de lista:
 * - quantity: Usado em listas do tipo Compras (mínimo 1)
 * - dueDate:  Usado em listas do tipo Tarefas
 * - url:      Usado em listas do tipo Wishlist (deve ser URL válida)
 */
public record CreateItemRequestDTO(
    @NotBlank(message = "Nome do item é obrigatório")
    @Size(max = 200, message = "Nome deve ter no máximo 200 caracteres")
    String name,

    @Min(value = 1, message = "Quantidade deve ser no mínimo 1")
    Integer quantity,

    LocalDateTime dueDate,

    @URL(message = "URL deve ser válida")
    @Size(max = 500, message = "URL deve ter no máximo 500 caracteres")
    String url,

    Integer position
) {
}
