package br.com.leoferolive.nossalista.activity.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record ActivityLogResponse(
    UUID id,
    UUID listId,
    UUID userId,
    String userName,
    String action,
    String targetType,
    UUID targetId,
    String targetName,
    Map<String, Object> details,
    LocalDateTime createdAt
) {
}
