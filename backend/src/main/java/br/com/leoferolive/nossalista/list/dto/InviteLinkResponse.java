package br.com.leoferolive.nossalista.list.dto;

import java.time.LocalDateTime;

/**
 * DTO de resposta para geração de link de convite.
 * <p>
 * Exemplo de resposta JSON:
 * <pre>
 * {
 *   "invite_code": "ABC123XYZ789",
 *   "invite_link": "https://nossalista.leoferolive.com.br/join/ABC123XYZ789",
 *   "expires_at": "2026-02-17T15:30:00"
 * }
 * </pre>
 *
 * @param invite_code Código de convite único (12 caracteres alfanuméricos uppercase)
 * @param invite_link URL completa para acessar a lista via convite
 * @param expires_at  Data/hora de expiração do link (24 horas após geração)
 */
public record InviteLinkResponse(
    String invite_code,
    String invite_link,
    LocalDateTime expires_at
) {
}
