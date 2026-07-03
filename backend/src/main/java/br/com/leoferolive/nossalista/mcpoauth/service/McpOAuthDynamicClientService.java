package br.com.leoferolive.nossalista.mcpoauth.service;

import br.com.leoferolive.nossalista.mcpoauth.config.McpOAuthProperties;
import br.com.leoferolive.nossalista.mcpoauth.domain.McpOAuthRegisteredClient;
import br.com.leoferolive.nossalista.mcpoauth.dto.ClientRegistrationRequest;
import br.com.leoferolive.nossalista.mcpoauth.dto.ClientRegistrationResponse;
import br.com.leoferolive.nossalista.mcpoauth.exception.OAuthClientRegistrationException;
import br.com.leoferolive.nossalista.mcpoauth.repository.McpOAuthRegisteredClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * {@code POST /oauth/register} — Dynamic Client Registration (RFC 7591) para
 * o servidor de autorização OAuth do MCP. Registro ABERTO (sem autenticação,
 * conforme a RFC) mas ENDURECIDO: {@code redirect_uris} validado (hardening
 * abaixo), rate limit por IP e teto global de clientes aplicados pelo
 * controller antes de chamar {@link #register}. Ver docs/DECISIONS.md D-024.
 *
 * <p>Todo cliente registrado aqui é PÚBLICO — {@code token_endpoint_auth_method}
 * é sempre fixado em {@code "none"} (ignorado o que o cliente pedir), sem
 * {@code client_secret} emitido, alinhado com
 * {@code token_endpoint_auth_methods_supported: ["none"]} do
 * {@code WellKnownOAuthController}. O consentimento (cookie + trava por
 * usuário, D-022) protege esses clientes exatamente como os estáticos —
 * nenhuma mudança em {@code McpOAuthAuthorizationService}.</p>
 */
@Service
public class McpOAuthDynamicClientService {

    private static final int CLIENT_ID_ENTROPY_BYTES = 16;
    private static final String CLIENT_ID_PREFIX = "dcr_";
    // "[::1]" (com colchetes): java.net.URI#getHost() preserva os colchetes de um
    // literal IPv6 na autoridade — ver McpOAuthClientRegistry, mesma lista.
    private static final List<String> LOOPBACK_HOSTS = List.of("localhost", "127.0.0.1", "[::1]");
    private static final List<String> SUPPORTED_GRANT_TYPES = List.of("authorization_code", "refresh_token");
    private static final List<String> DEFAULT_GRANT_TYPES = List.of("authorization_code", "refresh_token");
    private static final String DEFAULT_SCOPE = "read read_write";
    private static final String PUBLIC_CLIENT_AUTH_METHOD = "none";

    /** Mesmo limite da coluna {@code mcp_oauth_registered_clients.client_name} (migration V14). */
    private static final int MAX_CLIENT_NAME_LENGTH = 200;

    /** Mesmo limite da coluna {@code mcp_oauth_registered_clients.scope} (migration V14). */
    private static final int MAX_SCOPE_LENGTH = 50;

    private final McpOAuthRegisteredClientRepository repository;
    private final McpOAuthProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder idEncoder = Base64.getUrlEncoder().withoutPadding();

    public McpOAuthDynamicClientService(McpOAuthRegisteredClientRepository repository, McpOAuthProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Transactional
    public ClientRegistrationResponse register(ClientRegistrationRequest request) {
        // Corpo ausente (ex.: POST /oauth/register sem JSON algum) é tratado como
        // "sem redirect_uris" — mesmo erro RFC 7591 de um corpo presente mas vazio,
        // em vez de um 400 genérico de parsing. Guard explícito (em vez de deixar
        // implícito pelo caminho de validateRedirectUris(null)) para que o restante
        // do método possa assumir `request` não-nulo sem ambiguidade.
        if (request == null) {
            throw OAuthClientRegistrationException.invalidRedirectUri(
                "redirect_uris is required and must be a non-empty array.");
        }
        List<String> redirectUris = request.redirectUris();
        validateRedirectUris(redirectUris);
        List<String> grantTypes = resolveGrantTypes(request.grantTypes());
        // Sem isto, um client_name/scope maior que a coluna (achado do QA)
        // estourava DataIntegrityViolationException no repository.save() —
        // 500 genérico em vez de um 400 OAuth tratado, mesmo racional do
        // MAX_STATE_LENGTH de McpOAuthAuthorizeController.
        validateFieldLength(request.clientName(), MAX_CLIENT_NAME_LENGTH, "client_name");
        validateFieldLength(request.scope(), MAX_SCOPE_LENGTH, "scope");
        enforceRegisteredClientCap();

        McpOAuthRegisteredClient entity = new McpOAuthRegisteredClient();
        entity.setClientId(generateClientId());
        // @PrePersist também grava isto no INSERT real — setado aqui explicitamente
        // para que a resposta (client_id_issued_at) e os testes unitários (que usam
        // um repository mockado, sem lifecycle de persistência) tenham um valor
        // consistente sem depender do callback do JPA disparar.
        entity.setCreatedAt(LocalDateTime.now());
        entity.setRedirectUris(redirectUris);
        entity.setClientName(blankToNull(request.clientName()));
        // Fixo em "none": todo cliente DCR é público (PKCE-only), nunca confidencial —
        // ignora deliberadamente o que o cliente pediu (ver Javadoc da classe).
        entity.setTokenEndpointAuthMethod(PUBLIC_CLIENT_AUTH_METHOD);
        entity.setGrantTypes(grantTypes);
        entity.setScope(defaultIfBlank(request.scope(), DEFAULT_SCOPE));
        repository.save(entity);

        return ClientRegistrationResponse.from(entity);
    }

    /**
     * {@code redirect_uris} obrigatório e não-vazio; cada URI precisa ser
     * {@code https} (com host) OU um loopback ({@code http://localhost}/
     * {@code http://127.0.0.1}) com PORTA EXPLÍCITA (RFC 8252 §7.3) — rejeita
     * {@code http} remoto, {@code javascript:}/{@code data:} (nem https nem
     * loopback, caem no fallback), fragmentos ({@code #}) e wildcards de host.
     */
    private void validateRedirectUris(List<String> redirectUris) {
        if (redirectUris == null || redirectUris.isEmpty()) {
            throw OAuthClientRegistrationException.invalidRedirectUri(
                "redirect_uris is required and must be a non-empty array.");
        }
        for (String redirectUri : redirectUris) {
            validateSingleRedirectUri(redirectUri);
        }
    }

    private void validateSingleRedirectUri(String redirectUri) {
        validateSyntax(redirectUri);
        URI uri = parse(redirectUri);
        validateScheme(uri, redirectUri);
    }

    /** Checagens sobre a STRING crua, antes de tentar interpretá-la como {@link URI}. */
    private void validateSyntax(String redirectUri) {
        if (redirectUri == null || redirectUri.isBlank()) {
            throw OAuthClientRegistrationException.invalidRedirectUri("redirect_uris entries must not be blank.");
        }
        if (redirectUri.contains("#")) {
            throw OAuthClientRegistrationException.invalidRedirectUri(
                "redirect_uri must not contain a fragment: " + redirectUri);
        }
        if (redirectUri.contains("*")) {
            throw OAuthClientRegistrationException.invalidRedirectUri(
                "redirect_uri must not contain wildcards: " + redirectUri);
        }
    }

    private URI parse(String redirectUri) {
        try {
            return new URI(redirectUri);
        } catch (URISyntaxException e) {
            throw OAuthClientRegistrationException.invalidRedirectUri("redirect_uri is not a valid URI: " + redirectUri);
        }
    }

    /** {@code https} (com host) OU loopback http com porta explícita — nada mais é aceito. */
    private void validateScheme(URI uri, String redirectUri) {
        String scheme = uri.getScheme();
        if ("https".equalsIgnoreCase(scheme)) {
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw OAuthClientRegistrationException.invalidRedirectUri(
                    "https redirect_uri must have a host: " + redirectUri);
            }
            return;
        }
        if ("http".equalsIgnoreCase(scheme) && isLoopbackWithExplicitPort(uri)) {
            return;
        }
        throw OAuthClientRegistrationException.invalidRedirectUri(
            "redirect_uri must be https or a loopback address with an explicit port "
                + "(http://localhost:<port>/... or http://127.0.0.1:<port>/...): " + redirectUri);
    }

    private boolean isLoopbackWithExplicitPort(URI uri) {
        String host = uri.getHost();
        return host != null && LOOPBACK_HOSTS.contains(host.toLowerCase(Locale.ROOT)) && uri.getPort() > 0;
    }

    /**
     * Sem {@code grant_types} no pedido, assume o default (authorization_code +
     * refresh_token); qualquer grant type fora do conjunto suportado é rejeitado
     * (este servidor não implementa {@code client_credentials}, implicit, etc).
     *
     * <p>Deduplicado (preservando ordem) antes de persistir: sem isso, um pedido
     * repetindo o MESMO grant type dezenas de vezes (cada valor individualmente
     * válido) poderia inflar a coluna {@code grant_types} (VARCHAR(200)) além do
     * limite mesmo passando na checagem elemento-a-elemento — mesma classe de
     * achado do QA que motivou {@link #validateFieldLength}, fechada aqui pela
     * raiz (o conjunto suportado tem só 2 valores, então deduplicado nunca
     * chega perto do limite) em vez de mais uma checagem de tamanho.</p>
     */
    private List<String> resolveGrantTypes(List<String> requestedGrantTypes) {
        if (requestedGrantTypes == null || requestedGrantTypes.isEmpty()) {
            return DEFAULT_GRANT_TYPES;
        }
        List<String> deduplicated = new ArrayList<>();
        for (String grantType : requestedGrantTypes) {
            if (!SUPPORTED_GRANT_TYPES.contains(grantType)) {
                throw OAuthClientRegistrationException.invalidClientMetadata("Unsupported grant_type: " + grantType);
            }
            if (!deduplicated.contains(grantType)) {
                deduplicated.add(grantType);
            }
        }
        return deduplicated;
    }

    /**
     * Rejeita um campo de metadata (opcional, texto livre do cliente) maior que
     * o limite da coluna correspondente — ver {@link #MAX_CLIENT_NAME_LENGTH}/
     * {@link #MAX_SCOPE_LENGTH} e a migration V14.
     */
    private void validateFieldLength(String value, int maxLength, String fieldName) {
        if (value != null && value.length() > maxLength) {
            throw OAuthClientRegistrationException.invalidClientMetadata(
                fieldName + " exceeds the maximum supported length (" + maxLength + " characters).");
        }
    }

    /**
     * Teto global de clientes dinâmicos registrados — proteção contra flood do
     * banco por um script automatizado de registro (ver {@code app.mcp-oauth.dcr.max-registered-clients}).
     */
    private void enforceRegisteredClientCap() {
        long registered = repository.count();
        if (registered >= properties.getDcr().getMaxRegisteredClients()) {
            throw OAuthClientRegistrationException.temporarilyUnavailable(
                "Dynamic client registration is temporarily unavailable (registered client cap reached).");
        }
    }

    private String generateClientId() {
        byte[] bytes = new byte[CLIENT_ID_ENTROPY_BYTES];
        secureRandom.nextBytes(bytes);
        return CLIENT_ID_PREFIX + idEncoder.encodeToString(bytes);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
