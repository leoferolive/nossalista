package br.com.leoferolive.nossalista.list.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de requisição para criação de nova lista
 *
 * @param name  Nome da lista (obrigatório, 3-100 caracteres)
 * @param typeId ID do tipo da lista (1-4: Compras, Tarefas, Wishlist, Genérica)
 */
public record CreateListRequest(
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    String name,

    @NotNull(message = "Tipo de lista é obrigatório")
    @Min(value = 1, message = "Tipo de lista deve estar entre 1 e 4")
    @Max(value = 4, message = "Tipo de lista deve estar entre 1 e 4")
    Integer typeId
) {
}
