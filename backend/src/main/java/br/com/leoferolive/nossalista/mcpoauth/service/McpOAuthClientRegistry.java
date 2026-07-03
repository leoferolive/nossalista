package br.com.leoferolive.nossalista.mcpoauth.service;

import br.com.leoferolive.nossalista.mcpoauth.config.McpOAuthProperties;
import br.com.leoferolive.nossalista.mcpoauth.config.McpOAuthProperties.ClientDefinition;
import br.com.leoferolive.nossalista.mcpoauth.domain.McpOAuthRegisteredClient;
import br.com.leoferolive.nossalista.mcpoauth.exception.OAuthInvalidRedirectUriException;
import br.com.leoferolive.nossalista.mcpoauth.exception.OAuthUnknownClientException;
import br.com.leoferolive.nossalista.mcpoauth.repository.McpOAuthRegisteredClientRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Resolve clientes OAuth ESTÁTICOS (config, {@code app.mcp-oauth.clients}) e
 * DINÂMICOS (registrados via {@code POST /oauth/register}, Dynamic Client
 * Registration/RFC 7591 — ver docs/DECISIONS.md D-024) e valida
 * {@code redirect_uri}. Estáticos têm precedência por {@code id} — um cliente
 * dinâmico nunca pode colidir com um id estático (o prefixo {@code dcr_}
 * gerado pelo servidor já evita isso na prática).
 *
 * <p>Match de {@code redirect_uri} é sempre EXATO, exceto para clientes
 * ESTÁTICOS com {@link ClientDefinition#isAllowLoopbackRedirect()}, que aceitam
 * qualquer porta em {@code http://localhost/127.0.0.1/callback} (RFC 8252
 * §7.3) — regra explicitamente autorizada para clientes tipo Claude Code,
 * nunca um wildcard aberto de host ou path. Clientes DINÂMICOS usam sempre
 * match EXATO: o hardening de {@code McpOAuthDynamicClientService} já exige
 * porta explícita em {@code redirect_uris} loopback no registro, então a
 * exceção de porta variável do RFC 8252 não se aplica aqui.</p>
 */
@Component
public class McpOAuthClientRegistry {

    // "[::1]" (com colchetes): java.net.URI#getHost() preserva os colchetes de um
    // literal IPv6 na autoridade — ver McpOAuthDynamicClientService, mesma lista.
    private static final List<String> LOOPBACK_HOSTS = List.of("localhost", "127.0.0.1", "[::1]");
    private static final String LOOPBACK_PATH = "/callback";

    private final McpOAuthProperties properties;
    private final McpOAuthRegisteredClientRepository dynamicClientRepository;

    public McpOAuthClientRegistry(
        McpOAuthProperties properties, McpOAuthRegisteredClientRepository dynamicClientRepository
    ) {
        this.properties = properties;
        this.dynamicClientRepository = dynamicClientRepository;
    }

    /**
     * @param clientId id do cliente OAuth
     * @return a definição do cliente
     * @throws OAuthUnknownClientException se o {@code client_id} não estiver registrado
     */
    // @Transactional é NECESSÁRIO aqui: touchLastUsedAt (@Modifying) exige uma
    // transação ATIVA, e chamadores como McpOAuthAuthorizeController#authorize
    // não abrem nenhuma. noRollbackFor(OAuthUnknownClientException) evita um
    // segundo problema (achado deste bloqueio): SEM essa exclusão, um client_id
    // desconhecido, ao lançar essa exceção de dentro do advice transacional
    // deste método, marca a transação como rollback-only — mesmo quando este
    // método está PARTICIPANDO da transação de um chamador já-transacional
    // (ex.: McpOAuthTokenService#exchangeAuthorizationCode), que captura a
    // exceção logo em seguida e a converte para OAuthTokenException (coberta
    // pelo noRollbackFor de lá); o commit final falharia com
    // UnexpectedRollbackException porque a flag já teria sido setada aqui.
    @Transactional(noRollbackFor = OAuthUnknownClientException.class)
    public ClientDefinition require(String clientId) {
        Optional<ClientDefinition> staticClient = findStatic(clientId);
        if (staticClient.isPresent()) {
            return staticClient.get();
        }
        if (clientId != null) {
            Optional<McpOAuthRegisteredClient> dynamicClient = dynamicClientRepository.findByClientId(clientId);
            if (dynamicClient.isPresent()) {
                // "usado" (Fase C.1, D-024): a cada resolução bem-sucedida de um
                // client_id dinâmico em authorize/token, marca last_used_at — o
                // scheduler de limpeza só remove clientes NUNCA usados.
                dynamicClientRepository.touchLastUsedAt(clientId, LocalDateTime.now());
                return toClientDefinition(dynamicClient.get());
            }
        }
        throw new OAuthUnknownClientException(clientId);
    }

    public Optional<ClientDefinition> find(String clientId) {
        if (clientId == null) {
            return Optional.empty();
        }
        Optional<ClientDefinition> staticClient = findStatic(clientId);
        if (staticClient.isPresent()) {
            return staticClient;
        }
        return dynamicClientRepository.findByClientId(clientId).map(this::toClientDefinition);
    }

    private Optional<ClientDefinition> findStatic(String clientId) {
        if (clientId == null) {
            return Optional.empty();
        }
        return properties.getClients().stream()
            .filter(c -> clientId.equals(c.getId()))
            .findFirst();
    }

    /**
     * Adapta um cliente dinâmico persistido para o mesmo shape de
     * {@link ClientDefinition} usado pelos controllers/services de authorize e
     * token — nenhuma mudança de assinatura nesses chamadores. Match de
     * redirect_uri sempre EXATO ({@code allowLoopbackRedirect=false}, ver
     * Javadoc da classe).
     */
    private ClientDefinition toClientDefinition(McpOAuthRegisteredClient dynamicClient) {
        ClientDefinition definition = new ClientDefinition();
        definition.setId(dynamicClient.getClientId());
        String name = dynamicClient.getClientName();
        definition.setName(name != null && !name.isBlank() ? name : dynamicClient.getClientId());
        definition.setRedirectUris(dynamicClient.getRedirectUris());
        definition.setAllowLoopbackRedirect(false);
        return definition;
    }

    /**
     * Valida que {@code redirectUri} é permitido para o cliente informado.
     *
     * @throws OAuthInvalidRedirectUriException se não houver match exato nem, para
     *                                           clientes loopback, match da regra de porta variável
     */
    public void validateRedirectUri(ClientDefinition client, String redirectUri) {
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new OAuthInvalidRedirectUriException(client.getId(), redirectUri);
        }
        if (client.getRedirectUris().contains(redirectUri)) {
            return;
        }
        if (client.isAllowLoopbackRedirect() && isLoopbackCallback(redirectUri)) {
            return;
        }
        throw new OAuthInvalidRedirectUriException(client.getId(), redirectUri);
    }

    private boolean isLoopbackCallback(String redirectUri) {
        try {
            URI uri = new URI(redirectUri);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            String path = uri.getPath();
            return "http".equals(scheme)
                && host != null
                && LOOPBACK_HOSTS.contains(host.toLowerCase(Locale.ROOT))
                && LOOPBACK_PATH.equals(path);
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
