package br.com.leoferolive.nossalista.listitem.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * DTO de requisição para atualizar um item existente na lista
 *
 * Todos os campos são opcionais (PATCH semantics):
 * - Apenas campos fornecidos serão atualizados
 * - Campos omitidos mantêm o valor atual
 *
 * Campos dinâmicos por tipo de lista:
 * - quantity: Usado em listas do tipo Compras (mínimo 1)
 * - dueDate:  Usado em listas do tipo Tarefas
 * - url:      Usado em listas do tipo Wishlist (deve ser URL válida)
 */
public class UpdateItemRequest {

    @NotBlank(message = "Nome do item não pode estar vazio")
    @Size(max = 200, message = "Nome deve ter no máximo 200 caracteres")
    private String name;

    @Min(value = 1, message = "Quantidade deve ser no mínimo 1")
    private Integer quantity;

    @FutureOrPresent(message = "Data deve ser atual ou futura")
    private LocalDateTime dueDate;

    @Size(max = 500, message = "URL deve ter no máximo 500 caracteres")
    private String url;

    public UpdateItemRequest() {
    }

    public UpdateItemRequest(String name, Integer quantity, LocalDateTime dueDate, String url) {
        this.name = name;
        this.quantity = quantity;
        this.dueDate = dueDate;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
