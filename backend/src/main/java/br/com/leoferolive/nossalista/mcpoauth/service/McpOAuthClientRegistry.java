package br.com.leoferolive.nossalista.mcpoauth.service;

import br.com.leoferolive.nossalista.mcpoauth.config.McpOAuthProperties;
import br.com.leoferolive.nossalista.mcpoauth.config.McpOAuthProperties.ClientDefinition;
import br.com.leoferolive.nossalista.mcpoauth.exception.OAuthInvalidRedirectUriException;
import br.com.leoferolive.nossalista.mcpoauth.exception.OAuthUnknownClientException;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Resolve clientes OAuth estáticos (config, sem Dynamic Client Registration —
 * ver Passo 0 de docs/DECISIONS.md D-022) e valida {@code redirect_uri}.
 *
 * <p>Match de {@code redirect_uri} é sempre EXATO, exceto para clientes com
 * {@link ClientDefinition#isAllowLoopbackRedirect()}, que aceitam qualquer
 * porta em {@code http://localhost/127.0.0.1/callback} (RFC 8252 §7.3) — regra
 * explicitamente autorizada para clientes tipo Claude Code, nunca um wildcard
 * aberto de host ou path.</p>
 */
@Component
public class McpOAuthClientRegistry {

    private static final List<String> LOOPBACK_HOSTS = List.of("localhost", "127.0.0.1");
    private static final String LOOPBACK_PATH = "/callback";

    private final McpOAuthProperties properties;

    public McpOAuthClientRegistry(McpOAuthProperties properties) {
        this.properties = properties;
    }

    /**
     * @param clientId id do cliente OAuth
     * @return a definição do cliente
     * @throws OAuthUnknownClientException se o {@code client_id} não estiver registrado
     */
    public ClientDefinition require(String clientId) {
        return find(clientId).orElseThrow(() -> new OAuthUnknownClientException(clientId));
    }

    public Optional<ClientDefinition> find(String clientId) {
        if (clientId == null) {
            return Optional.empty();
        }
        return properties.getClients().stream()
            .filter(c -> clientId.equals(c.getId()))
            .findFirst();
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
