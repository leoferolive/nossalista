package br.com.leoferolive.nossalista.mcpoauth.repository;

import br.com.leoferolive.nossalista.mcpoauth.domain.McpOAuthRegisteredClient;
import br.com.leoferolive.nossalista.support.AbstractPostgresIT;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de repositório de {@link McpOAuthRegisteredClientRepository} — mesmo
 * padrão de {@code McpOAuthAtomicUpdatesRepositoryTest} ({@code @SpringBootTest}
 * + {@code @Transactional}, rollback automático no schema compartilhado do
 * container Postgres).
 *
 * <p>Estende {@link AbstractPostgresIT}: roda contra PostgreSQL real
 * (Testcontainers), não H2 — ver T1 da Onda 2 (honestidade de métrica).</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class McpOAuthRegisteredClientRepositoryTest extends AbstractPostgresIT {

    @Autowired
    private McpOAuthRegisteredClientRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("touchLastUsedAt grava last_used_at para o client_id informado")
    void touchLastUsedAtUpdatesTimestamp() {
        McpOAuthRegisteredClient client = persistClient();
        assertThat(client.getLastUsedAt()).isNull();

        LocalDateTime now = LocalDateTime.now();
        repository.touchLastUsedAt(client.getClientId(), now);
        // O UPDATE em massa acima não sincroniza o cache de 1º nível do Hibernate —
        // sem isso, o findByClientId abaixo devolveria a instância cacheada (com
        // lastUsedAt ainda null), mesmo padrão documentado nos repositórios irmãos
        // deste módulo (ver Javadoc de PendingAuthorizationRepository).
        entityManager.clear();

        McpOAuthRegisteredClient reloaded = repository.findByClientId(client.getClientId()).orElseThrow();
        assertThat(reloaded.getLastUsedAt()).isEqualToIgnoringNanos(now);
    }

    @Test
    @DisplayName("touchLastUsedAt é um no-op silencioso para um client_id desconhecido")
    void touchLastUsedAtIsNoOpForUnknownClient() {
        repository.touchLastUsedAt("does-not-exist", LocalDateTime.now());
        // não lança -> passou
    }

    @Test
    @DisplayName("deleteUnusedRegisteredBefore remove clientes NUNCA usados registrados antes do cutoff")
    void deleteUnusedRegisteredBeforeRemovesNeverUsedOldClients() {
        McpOAuthRegisteredClient neverUsedOld = persistClient();
        backdateCreatedAt(neverUsedOld, LocalDateTime.now().minusDays(31));

        repository.deleteUnusedRegisteredBefore(LocalDateTime.now().minusDays(30));

        assertThat(repository.findByClientId(neverUsedOld.getClientId())).isEmpty();
    }

    @Test
    @DisplayName("deleteUnusedRegisteredBefore preserva um cliente já USADO, mesmo registrado há muito tempo")
    void deleteUnusedRegisteredBeforePreservesUsedClients() {
        McpOAuthRegisteredClient usedOld = persistClient();
        backdateCreatedAt(usedOld, LocalDateTime.now().minusDays(31));
        repository.touchLastUsedAt(usedOld.getClientId(), LocalDateTime.now().minusDays(20));

        repository.deleteUnusedRegisteredBefore(LocalDateTime.now().minusDays(30));

        assertThat(repository.findByClientId(usedOld.getClientId())).isPresent();
    }

    @Test
    @DisplayName("deleteUnusedRegisteredBefore preserva um cliente NUNCA usado, mas ainda dentro do TTL")
    void deleteUnusedRegisteredBeforePreservesRecentUnusedClients() {
        McpOAuthRegisteredClient recentUnused = persistClient();

        repository.deleteUnusedRegisteredBefore(LocalDateTime.now().minusDays(30));

        assertThat(repository.findByClientId(recentUnused.getClientId())).isPresent();
    }

    private McpOAuthRegisteredClient persistClient() {
        McpOAuthRegisteredClient client = new McpOAuthRegisteredClient();
        client.setClientId("dcr_" + UUID.randomUUID());
        client.setRedirectUris(List.of("https://app.example.com/callback"));
        client.setScope("read read_write");
        client.setTokenEndpointAuthMethod("none");
        client.setGrantTypes(List.of("authorization_code", "refresh_token"));
        return repository.saveAndFlush(client);
    }

    /**
     * Contorna o {@code @PrePersist} (que sempre grava {@code createdAt=now()})
     * e o {@code updatable=false} da coluna (que faria um {@code save()} comum
     * ignorar a mudança) via um UPDATE JPQL direto — necessário para simular um
     * cliente registrado há N dias sem esperar dias reais.
     */
    private void backdateCreatedAt(McpOAuthRegisteredClient client, LocalDateTime createdAt) {
        entityManager.createQuery("UPDATE McpOAuthRegisteredClient c SET c.createdAt = :createdAt WHERE c.id = :id")
            .setParameter("createdAt", createdAt)
            .setParameter("id", client.getId())
            .executeUpdate();
        entityManager.clear();
    }
}
