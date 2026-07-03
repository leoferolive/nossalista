package br.com.leoferolive.nossalista.mcpoauth.dto;

/**
 * Resposta dos endpoints de decisão de consentimento
 * ({@code /api/oauth/consent/{requestId}/approve|deny}): a SPA navega o
 * browser para {@code redirectUrl} para devolver o controle ao cliente OAuth
 * (claude.ai, Claude Code), com {@code code}+{@code state} (aprovação) ou
 * {@code error}+{@code state} (negação).
 */
public record ConsentDecisionResponse(String redirectUrl) {
}
