package br.com.leoferolive.nossalista.mcpoauth.service;

import br.com.leoferolive.nossalista.apitoken.domain.TokenScope;
import br.com.leoferolive.nossalista.mcpoauth.config.McpOAuthProperties;
import br.com.leoferolive.nossalista.mcpoauth.config.McpOAuthProperties.ClientDefinition;
import br.com.leoferolive.nossalista.mcpoauth.domain.McpOAuthCode;
import br.com.leoferolive.nossalista.mcpoauth.domain.PendingAuthorization;
import br.com.leoferolive.nossalista.mcpoauth.dto.PendingAuthorizationView;
import br.com.leoferolive.nossalista.mcpoauth.exception.OAuthConsentForbiddenException;
import br.com.leoferolive.nossalista.mcpoauth.exception.PendingAuthorizationNotFoundException;
import br.com.leoferolive.nossalista.mcpoauth.repository.McpOAuthCodeRepository;
import br.com.leoferolive.nossalista.mcpoauth.repository.PendingAuthorizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Orquestra o fluxo de consentimento OAuth: criação do pedido pendente (no
 * {@code GET /oauth/authorize}), exibição na tela de consentimento da SPA, e
 * decisão do usuário (aprovar → emite authorization code; negar → erro
 * {@code access_denied}), sempre devolvendo uma URL de redirect pronta para o
 * cliente OAuth (claude.ai, Claude Code).
 */
@Service
public class McpOAuthAuthorizationService {

    /**
     * Nome do cookie HttpOnly/Secure/SameSite=Lax que vincula o pedido de
     * autorização ao browser que chamou {@code GET /oauth/authorize} — ver
     * {@link PendingAuthorization#getNonce()} e o Javadoc da classe.
     */
    public static final String CONSENT_COOKIE_NAME = "mcp_oauth_consent";

    /** 32 bytes = 256 bits de entropia, mesmo padrão de {@code OAuthCodeStore} (D-011) e PAT (D-018). */
    private static final int CODE_BYTES = 32;

    /** 32 bytes = 256 bits de entropia para o nonce de vínculo do cookie de consentimento. */
    private static final int NONCE_BYTES = 32;

    private final McpOAuthProperties properties;
    private final McpOAuthClientRegistry clientRegistry;
    private final PendingAuthorizationRepository pendingRepository;
    private final McpOAuthCodeRepository codeRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder codeEncoder = Base64.getUrlEncoder().withoutPadding();

    public McpOAuthAuthorizationService(
        McpOAuthProperties properties,
        McpOAuthClientRegistry clientRegistry,
        PendingAuthorizationRepository pendingRepository,
        McpOAuthCodeRepository codeRepository
    ) {
        this.properties = properties;
        this.clientRegistry = clientRegistry;
        this.pendingRepository = pendingRepository;
        this.codeRepository = codeRepository;
    }

    /**
     * @return o {@link PendingAuthorization} criado — o chamador (controller)
     *         usa {@link PendingAuthorization#getNonce()} para setar o cookie
     *         de vínculo no browser que originou o pedido (ver classe/D-022).
     */
    @Transactional
    public PendingAuthorization createPending(
        String clientId, String redirectUri, TokenScope scope, String state, String codeChallenge, String resource
    ) {
        PendingAuthorization pending = new PendingAuthorization();
        pending.setClientId(clientId);
        pending.setRedirectUri(redirectUri);
        pending.setScope(scope);
        pending.setState(state);
        pending.setCodeChallenge(codeChallenge);
        pending.setResource(resource);
        pending.setNonce(generateNonce());
        pending.setExpiresAt(LocalDateTime.now().plus(properties.getPendingAuthorizationTtl()));
        return pendingRepository.save(pending);
    }

    /**
     * Carrega os dados do pedido para a tela de consentimento. Reivindica o
     * pedido para {@code userId} se ainda não reivindicado; se já reivindicado
     * por OUTRO usuário, rejeita (403) — impede que a vítima de um link de
     * phishing sequer veja os detalhes de um pedido gerado por um atacante.
     */
    @Transactional
    public PendingAuthorizationView view(UUID requestId, UUID userId) {
        PendingAuthorization pending = requirePending(requestId);
        claimOrVerifyOwnership(pending, userId);

        String clientName = clientRegistry.find(pending.getClientId())
            .map(ClientDefinition::getName)
            .orElse(pending.getClientId());
        return new PendingAuthorizationView(
            pending.getId(), pending.getClientId(), clientName, pending.getScope(), hostOf(pending.getRedirectUri()));
    }

    /**
     * Usuário aprovou o consentimento: emite o authorization code e devolve a
     * URL de redirect ({@code redirect_uri?code=...&state=...}) para a SPA
     * navegar o browser de volta ao cliente OAuth.
     *
     * @param cookieNonce valor do cookie de vínculo enviado pelo browser (ver
     *                    {@link #createPending}); precisa bater com
     *                    {@link PendingAuthorization#getNonce()}
     */
    @Transactional
    public String approve(UUID requestId, UUID userId, String cookieNonce) {
        PendingAuthorization pending = requirePending(requestId);
        requireValidCookie(pending, cookieNonce);
        claimOrVerifyOwnership(pending, userId);

        McpOAuthCode code = new McpOAuthCode();
        code.setCode(generateCode());
        code.setUserId(userId);
        code.setClientId(pending.getClientId());
        code.setRedirectUri(pending.getRedirectUri());
        code.setScope(pending.getScope());
        code.setCodeChallenge(pending.getCodeChallenge());
        code.setResource(pending.getResource());
        code.setExpiresAt(LocalDateTime.now().plus(properties.getCodeTtl()));
        codeRepository.save(code);

        pendingRepository.delete(pending);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("code", code.getCode());
        params.put("state", pending.getState());
        return buildRedirect(pending.getRedirectUri(), params);
    }

    /**
     * Usuário negou o consentimento: devolve a URL de redirect com
     * {@code error=access_denied} (RFC 6749 §4.1.2.1), sem emitir code algum.
     *
     * @param cookieNonce ver {@link #approve}
     */
    @Transactional
    public String deny(UUID requestId, UUID userId, String cookieNonce) {
        PendingAuthorization pending = requirePending(requestId);
        requireValidCookie(pending, cookieNonce);
        claimOrVerifyOwnership(pending, userId);

        pendingRepository.delete(pending);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("error", "access_denied");
        params.put("state", pending.getState());
        return buildRedirect(pending.getRedirectUri(), params);
    }

    private PendingAuthorization requirePending(UUID requestId) {
        PendingAuthorization pending = pendingRepository.findById(requestId)
            .orElseThrow(() -> new PendingAuthorizationNotFoundException(
                "Authorization request not found, already decided, or expired."));
        if (pending.isExpired(LocalDateTime.now())) {
            pendingRepository.delete(pending);
            throw new PendingAuthorizationNotFoundException("Authorization request expired.");
        }
        return pending;
    }

    /**
     * Reivindica o pedido para {@code userId} na primeira chamada (view ou
     * decisão direta); em chamadas seguintes, exige que seja o MESMO usuário —
     * caso contrário, 403 (defesa contra sequestro de consentimento cross-user).
     *
     * <p><b>Reivindicação atômica (achado do review, M-1):</b> o UPDATE
     * condicional {@link PendingAuthorizationRepository#claimIfUnclaimed} evita
     * que duas contas diferentes, chamando quase simultaneamente para o MESMO
     * pedido ainda não reivindicado, acabem ambas "vencendo" a reivindicação em
     * sequência (a segunda `save()` sobrescrevendo silenciosamente a primeira,
     * sob o padrão anterior de ler-decidir-gravar em Java). Quando o UPDATE não
     * afeta nenhuma linha, a leitura de conferência usa
     * {@link PendingAuthorizationRepository#findClaimedByUserId} — uma projeção
     * ESCALAR, não a entidade — porque {@code pending} já está no contexto de
     * persistência desta transação e um `findById` comum devolveria a instância
     * em cache do 1º nível, sem refletir o UPDATE que acabou de rodar.</p>
     */
    private void claimOrVerifyOwnership(PendingAuthorization pending, UUID userId) {
        int claimed = pendingRepository.claimIfUnclaimed(pending.getId(), userId);
        if (claimed > 0) {
            pending.setClaimedByUserId(userId);
            return;
        }
        UUID claimedBy = pendingRepository.findClaimedByUserId(pending.getId()).orElse(null);
        if (claimedBy == null || !claimedBy.equals(userId)) {
            throw new OAuthConsentForbiddenException(
                "This authorization request was already claimed by a different account.");
        }
    }

    /**
     * Exige que o cookie de vínculo do browser bata com o nonce persistido no
     * pedido — só o browser que chamou {@code GET /oauth/authorize} o possui.
     * Comparação em tempo constante (mesmo padrão de {@link PkceValidator}).
     */
    private void requireValidCookie(PendingAuthorization pending, String cookieNonce) {
        boolean valid = cookieNonce != null
            && MessageDigest.isEqual(
                cookieNonce.getBytes(StandardCharsets.UTF_8), pending.getNonce().getBytes(StandardCharsets.UTF_8));
        if (!valid) {
            throw new OAuthConsentForbiddenException(
                "Missing or invalid consent binding. Restart the connection from the OAuth client.");
        }
    }

    private String generateCode() {
        byte[] bytes = new byte[CODE_BYTES];
        secureRandom.nextBytes(bytes);
        return codeEncoder.encodeToString(bytes);
    }

    private String generateNonce() {
        byte[] bytes = new byte[NONCE_BYTES];
        secureRandom.nextBytes(bytes);
        return codeEncoder.encodeToString(bytes);
    }

    private String buildRedirect(String redirectUri, Map<String, String> params) {
        StringBuilder sb = new StringBuilder(redirectUri);
        sb.append(redirectUri.contains("?") ? '&' : '?');
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            if (!first) {
                sb.append('&');
            }
            sb.append(entry.getKey()).append('=')
                .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            first = false;
        }
        return sb.toString();
    }

    private String hostOf(String redirectUri) {
        try {
            return new URI(redirectUri).getHost();
        } catch (URISyntaxException e) {
            return redirectUri;
        }
    }
}
