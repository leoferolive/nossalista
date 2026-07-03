package br.com.leoferolive.nossalista.mcp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

/**
 * Resultado de {@code share_list}. Campos são preenchidos conforme o modo:
 * {@code mode="username"} preenche {@code invitedUsername}/{@code message};
 * {@code mode="link"} preenche {@code inviteCode}/{@code inviteLink}/{@code expiresAt}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ShareListResult(
    String mode,
    @Nullable String invitedUsername,
    @Nullable String message,
    @Nullable String inviteCode,
    @Nullable String inviteLink,
    @Nullable String expiresAt
) {
}
