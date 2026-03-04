package br.com.leoferolive.nossalista.list.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ListStateResponse(
    UUID listId,
    Long revision,
    LocalDateTime updatedAt,
    Long itemsCount
) {
}
