package br.com.leoferolive.nossalista.member.dto;

/**
 * DTO de requisição para entrar em uma lista via convite.
 * <p>
 * Este DTO é vazio pois o endpoint POST /api/lists/join/{inviteCode}
 * não requer body na requisição. Todas as informações necessárias
 * (invite_code) são fornecidas via path variable e o usuário
 * é identificado via JWT token no header Authorization.
 */
public record JoinListRequest() {
    // Record vazio - endpoint não requer body
}
