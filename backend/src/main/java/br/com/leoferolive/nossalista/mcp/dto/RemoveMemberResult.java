package br.com.leoferolive.nossalista.mcp.dto;

/**
 * @param action {@code "REMOVED"} quando o dono removeu outro usuário, ou
 *               {@code "LEFT"} quando o próprio usuário saiu da lista
 */
public record RemoveMemberResult(
    String userId,
    String action
) {
}
