package br.com.leoferolive.nossalista.mcp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

/**
 * {@code @JsonInclude(NON_NULL)}: campos opcionais nulos são omitidos da
 * serialização — sem isso, o valor {@code null} explícito no JSON falha a
 * validação do outputSchema (que tipa a propriedade como string/number, não
 * como union com null, mesmo quando ela não está em "required").
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ItemSummary(
    String id,
    String name,
    boolean checked,
    @Nullable Integer quantity,
    @Nullable String dueDate,
    @Nullable String url,
    Integer position
) {
}
