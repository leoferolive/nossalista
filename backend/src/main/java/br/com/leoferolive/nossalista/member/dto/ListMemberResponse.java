package br.com.leoferolive.nossalista.member.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ListMemberResponse(
    UserSummaryResponse user,
    String role,
    LocalDateTime joined_at
) {
    public record UserSummaryResponse(
        UUID id,
        String username,
        String name,
        String avatar_url
    ) {
    }
}
