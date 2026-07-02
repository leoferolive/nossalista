package br.com.leoferolive.nossalista.apitoken.service;

import br.com.leoferolive.nossalista.apitoken.domain.PersonalAccessToken;
import br.com.leoferolive.nossalista.apitoken.dto.CreatePersonalAccessTokenRequest;
import br.com.leoferolive.nossalista.apitoken.dto.PersonalAccessTokenCreatedResponse;
import br.com.leoferolive.nossalista.apitoken.dto.PersonalAccessTokenMapper;
import br.com.leoferolive.nossalista.apitoken.dto.PersonalAccessTokenResponse;
import br.com.leoferolive.nossalista.apitoken.exception.PersonalAccessTokenLimitExceededException;
import br.com.leoferolive.nossalista.apitoken.exception.PersonalAccessTokenNotFoundException;
import br.com.leoferolive.nossalista.apitoken.repository.PersonalAccessTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço de Personal Access Tokens (PAT) — geração, listagem, revogação e
 * autenticação de tokens usados por clientes MCP/API externos.
 *
 * <p>O valor em claro do token é gerado com {@link SecureRandom} (256 bits de
 * entropia, mesmo padrão de {@code OAuthCodeStore}), prefixado com
 * {@value #TOKEN_PREFIX}, e SÓ o hash SHA-256 é persistido — o valor em claro
 * é devolvido uma única vez na criação e nunca pode ser recuperado depois
 * (decisão D-018, ver {@code docs/DECISIONS.md}).</p>
 */
@Service
public class PersonalAccessTokenService {

    /** Prefixo público do token, usado para reconhecer PATs no filtro de autenticação. */
    public static final String TOKEN_PREFIX = "nlmcp_";

    /** 32 bytes = 256 bits de entropia para o segredo do token. */
    static final int TOKEN_ENTROPY_BYTES = 32;

    /** Limite de tokens ativos (não revogados) por usuário. */
    static final int MAX_ACTIVE_TOKENS_PER_USER = 10;

    /** Janela mínima entre atualizações de {@code last_used_at} — evita write a cada request. */
    static final Duration LAST_USED_THROTTLE = Duration.ofSeconds(60);

    /** Quantidade de caracteres do segredo exibidos no prefixo (ex.: {@code nlmcp_a1b2c3}). */
    private static final int PREFIX_DISPLAY_CHARS = 6;

    private final PersonalAccessTokenRepository repository;
    private final PersonalAccessTokenMapper mapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public PersonalAccessTokenService(PersonalAccessTokenRepository repository, PersonalAccessTokenMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * Cria um novo PAT para o usuário informado.
     *
     * @param userId  ID do usuário dono do token
     * @param request dados de criação (nome, escopo, validade opcional)
     * @return resposta contendo o token em claro (única exposição) e metadados
     * @throws PersonalAccessTokenLimitExceededException se o limite de tokens ativos for atingido
     */
    @Transactional
    public PersonalAccessTokenCreatedResponse create(UUID userId, CreatePersonalAccessTokenRequest request) {
        long activeCount = repository.countByUserIdAndRevokedAtIsNull(userId);
        if (activeCount >= MAX_ACTIVE_TOKENS_PER_USER) {
            throw new PersonalAccessTokenLimitExceededException(MAX_ACTIVE_TOKENS_PER_USER);
        }

        byte[] entropy = new byte[TOKEN_ENTROPY_BYTES];
        secureRandom.nextBytes(entropy);
        String secret = HexFormat.of().formatHex(entropy);
        String rawToken = TOKEN_PREFIX + secret;

        PersonalAccessToken entity = new PersonalAccessToken();
        entity.setUserId(userId);
        entity.setName(request.name());
        entity.setTokenHash(sha256Hex(rawToken));
        entity.setPrefix(TOKEN_PREFIX + secret.substring(0, PREFIX_DISPLAY_CHARS));
        entity.setScope(request.scope());
        if (request.expiresInDays() != null) {
            entity.setExpiresAt(LocalDateTime.now().plusDays(request.expiresInDays()));
        }

        PersonalAccessToken saved = repository.save(entity);
        return mapper.toCreatedResponse(saved, rawToken);
    }

    /**
     * Lista os tokens (metadados, sem hash) do usuário informado.
     *
     * @param userId ID do usuário
     * @return lista de tokens, mais recentes primeiro
     */
    @Transactional(readOnly = true)
    public List<PersonalAccessTokenResponse> list(UUID userId) {
        return repository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(mapper::toResponse)
            .toList();
    }

    /**
     * Revoga um token do usuário. Idempotente: revogar um token já revogado
     * não é erro.
     *
     * @param tokenId ID do token
     * @param userId  ID do usuário dono esperado
     * @throws PersonalAccessTokenNotFoundException se o token não existir ou pertencer a outro usuário
     */
    @Transactional
    public void revoke(UUID tokenId, UUID userId) {
        PersonalAccessToken token = repository.findByIdAndUserId(tokenId, userId)
            .orElseThrow(() -> new PersonalAccessTokenNotFoundException("Token não encontrado"));

        if (token.getRevokedAt() == null) {
            token.setRevokedAt(LocalDateTime.now());
            repository.save(token);
        }
    }

    /**
     * Resolve e valida um token PAT em claro para autenticação.
     *
     * <p>Atualiza {@code last_used_at} com throttle de {@link #LAST_USED_THROTTLE}
     * (só grava se a última atualização foi há mais tempo que isso), para não
     * escrever no banco a cada requisição autenticada via PAT.</p>
     *
     * @param rawToken valor em claro recebido no header {@code Authorization: Bearer}
     * @return Optional com o token se válido (existe, não revogado, não expirado); vazio caso contrário
     */
    @Transactional
    public Optional<PersonalAccessToken> authenticate(String rawToken) {
        if (rawToken == null || !rawToken.startsWith(TOKEN_PREFIX)) {
            return Optional.empty();
        }

        Optional<PersonalAccessToken> found = repository.findByTokenHash(sha256Hex(rawToken));
        if (found.isEmpty()) {
            return Optional.empty();
        }

        PersonalAccessToken token = found.get();
        LocalDateTime now = LocalDateTime.now();
        if (!token.isActive(now)) {
            return Optional.empty();
        }

        touchLastUsed(token, now);
        return Optional.of(token);
    }

    private void touchLastUsed(PersonalAccessToken token, LocalDateTime now) {
        LocalDateTime lastUsed = token.getLastUsedAt();
        boolean throttled = lastUsed != null && Duration.between(lastUsed, now).compareTo(LAST_USED_THROTTLE) < 0;
        if (throttled) {
            return;
        }
        token.setLastUsedAt(now);
        repository.save(token);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível na JVM", e);
        }
    }
}
