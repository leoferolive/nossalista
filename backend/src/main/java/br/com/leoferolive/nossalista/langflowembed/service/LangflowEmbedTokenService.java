package br.com.leoferolive.nossalista.langflowembed.service;

import br.com.leoferolive.nossalista.langflowembed.config.LangflowEmbedProperties;
import br.com.leoferolive.nossalista.user.domain.User;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;

/**
 * Emite o JWT RS256 curto que o Langflow recebe no cookie do embed.
 *
 * <p>Contrato das claims em {@code docs/langflow-embed.md}. O token não carrega
 * segredo de sessão: quem o possui só alcança o Langflow até o {@code exp}.</p>
 */
@Service
public class LangflowEmbedTokenService {

    private final LangflowEmbedKeyService keyService;
    private final LangflowEmbedProperties properties;
    private final Clock clock;

    @Autowired
    public LangflowEmbedTokenService(LangflowEmbedKeyService keyService, LangflowEmbedProperties properties) {
        this(keyService, properties, Clock.systemUTC());
    }

    LangflowEmbedTokenService(LangflowEmbedKeyService keyService, LangflowEmbedProperties properties, Clock clock) {
        this.keyService = keyService;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Emite um token para o usuário autenticado, com {@code exp = iat + token-ttl}.
     *
     * <p>Exemplo: {@code String token = tokenService.issue(user);}</p>
     *
     * @param user usuário já autenticado pela sessão do NossaLista
     * @return JWT compacto assinado com a chave do embed
     */
    public String issue(User user) {
        Instant now = clock.instant();
        return Jwts.builder()
            .header().keyId(keyService.keyId()).and()
            .issuer(properties.getIssuer())
            .subject(user.getId().toString())
            .audience().single(properties.getAudience())
            .claim("email", user.getEmail())
            .claim("preferred_username", user.getUsername())
            .claim("name", displayName(user))
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(properties.getTokenTtl())))
            .signWith(keyService.privateKey(), Jwts.SIG.RS256)
            .compact();
    }

    private String displayName(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            return user.getUsername();
        }
        return user.getName();
    }
}
