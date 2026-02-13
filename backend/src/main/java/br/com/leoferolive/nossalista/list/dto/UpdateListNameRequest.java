package br.com.leoferolive.nossalista.list.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de requisição para atualização do nome da lista
 *
 * @param name Novo nome da lista (obrigatório, 3-100 caracteres)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateListNameRequest(
    @NotBlank(message = "Nome da lista é obrigatório")
    @Size(min = 3, max = 100, message = "Nome da lista deve ter entre 3 e 100 caracteres")
    String name
) {
}
