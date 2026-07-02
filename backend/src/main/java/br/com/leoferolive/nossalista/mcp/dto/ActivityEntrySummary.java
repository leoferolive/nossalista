package br.com.leoferolive.nossalista.mcp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ActivityEntrySummary(
    String id,
    String userName,
    String action,
    @Nullable String targetType,
    @Nullable String targetName,
    @Nullable Map<String, Object> details,
    String createdAt
) {
}
