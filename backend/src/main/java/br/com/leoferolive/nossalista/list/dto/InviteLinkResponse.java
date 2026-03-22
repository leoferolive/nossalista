package br.com.leoferolive.nossalista.list.dto;

import java.time.LocalDateTime;

/**
 * DTO de resposta para geração de link de convite.
 * <p>
 * Exemplo de resposta JSON:
 * <pre>
 * {
 *   "inviteCode": "ABC123XYZ789",
 *   "inviteLink": "https://nossalista.leoferolive.com.br/join/ABC123XYZ789",
 *   "expiresAt": "2026-02-17T15:30:00"
 * }
 * </pre>
 *
 * @param inviteCode Código de convite único (12 caracteres alfanuméricos uppercase)
 * @param inviteLink URL completa para acessar a lista via convite
 * @param expiresAt  Data/hora de expiração do link (24 horas após geração)
 */
public record InviteLinkResponse(
    String inviteCode,
    String inviteLink,
    LocalDateTime expiresAt
) {
}
