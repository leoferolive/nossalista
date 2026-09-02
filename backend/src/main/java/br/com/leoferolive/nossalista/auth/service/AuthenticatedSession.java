package br.com.leoferolive.nossalista.auth.service;

import br.com.leoferolive.nossalista.user.domain.User;

/**
 * Sessão recém-autenticada antes de o token ser persistido exclusivamente no
 * cookie HttpOnly da resposta.
 */
public record AuthenticatedSession(User user, String token) {
}
