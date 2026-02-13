package br.com.leoferolive.nossalista.listitem.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * DTO de requisição para atualizar um item existente
 *
 * Todos os campos são opcionais - apenas os campos fornecidos serão atualizados
 */
public record UpdateItemRequestDTO(
    @Size(max = 200, message = "Nome deve ter no máximo 200 caracteres")
    String name,

    Boolean checked,

    Integer quantity,

    LocalDateTime dueDate,

    @Size(max = 500, message = "URL deve ter no máximo 500 caracteres")
    String url,

    Integer position
) {
}
