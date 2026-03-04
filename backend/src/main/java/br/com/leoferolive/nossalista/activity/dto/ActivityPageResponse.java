package br.com.leoferolive.nossalista.activity.dto;

import org.springframework.data.domain.Page;

public record ActivityPageResponse(
    java.util.List<ActivityLogResponse> content,
    int totalPages,
    long totalElements,
    int size,
    int number,
    boolean first,
    boolean last
) {
    public static ActivityPageResponse from(Page<ActivityLogResponse> page) {
        return new ActivityPageResponse(
            page.getContent(),
            page.getTotalPages(),
            page.getTotalElements(),
            page.getSize(),
            page.getNumber(),
            page.isFirst(),
            page.isLast()
        );
    }
}
