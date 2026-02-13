package br.com.leoferolive.nossalista.listitem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * DTO de requisição para criar um novo item na lista
 *
 * Campos dinâmicos por tipo de lista:
 * - quantity: Usado em listas do tipo Compras
 * - dueDate:  Usado em listas do tipo Tarefas
 * - url:      Usado em listas do tipo Wishlist
 */
public record CreateItemRequestDTO(
    @NotBlank(message = "Nome do item é obrigatório")
    @Size(max = 200, message = "Nome deve ter no máximo 200 caracteres")
    String name,

    Integer quantity,

    LocalDateTime dueDate,

    @Size(max = 500, message = "URL deve ter no máximo 500 caracteres")
    String url,

    Integer position
) {
}
