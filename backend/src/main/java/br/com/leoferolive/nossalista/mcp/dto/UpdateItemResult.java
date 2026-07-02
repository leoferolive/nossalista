package br.com.leoferolive.nossalista.mcp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UpdateItemResult(
    String id,
    String name,
    boolean checked,
    @Nullable Integer quantity,
    @Nullable String dueDate,
    @Nullable String url
) {
}
