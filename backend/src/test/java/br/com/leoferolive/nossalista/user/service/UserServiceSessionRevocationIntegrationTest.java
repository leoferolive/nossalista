package br.com.leoferolive.nossalista.user.service;

import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.domain.User;
import br.com.leoferolive.nossalista.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regressão: revogação de sessão (logout / reset de senha) não pode ser
 * desfeita por gravações concorrentes de {@link User} baseadas em estado antigo
 * (updateProfile, markEmailVerified, identidade Google).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@DisplayName("UserService session revocation vs. stale saves")
class UserServiceSessionRevocationIntegrationTest {

    private static final int REVOKERS = 12;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("save de entidade antiga não restaura session_version após logout")
    void staleSaveDoesNotRestoreSessionVersionAfterRevoke() {
        UUID userId = createUser();
        User stale = userService.findById(userId).orElseThrow();

        userService.revokeWebSessions(userId);
        stale.setName("Nome atualizado por fluxo concorrente");
        userService.save(stale);

        assertThat(currentSessionVersion(userId)).isEqualTo(1);
    }

    @Test
    @DisplayName("save de entidade antiga não restaura senha nem session_version após reset")
    void staleSaveDoesNotRestorePasswordOrSessionVersionAfterReset() {
        UUID userId = createUser();
        User stale = userService.findById(userId).orElseThrow();

        userService.updatePassword(userId, "new-hash");
        stale.setEmailVerified(true);
        userService.save(stale);

        User reloaded = userService.findById(userId).orElseThrow();
        assertThat(reloaded.getPassword()).isEqualTo("new-hash");
        assertThat(reloaded.getSessionVersion()).isEqualTo(1);
        assertThat(reloaded.isEmailVerified()).isTrue();
    }

    @Test
    @DisplayName("revogações concorrentes incrementam session_version sem perder nenhuma")
    void concurrentRevocationsAreNotLost() throws Exception {
        UUID userId = createUser();
        ExecutorService executor = Executors.newFixedThreadPool(REVOKERS);
        try {
            CountDownLatch ready = new CountDownLatch(REVOKERS);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> revocations = new ArrayList<>();
            for (int i = 0; i < REVOKERS; i++) {
                revocations.add(executor.submit(() -> revokeWhenStarted(userId, ready, start)));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<?> revocation : revocations) {
                revocation.get(30, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(currentSessionVersion(userId)).isEqualTo(REVOKERS);
    }

    private Void revokeWhenStarted(UUID userId, CountDownLatch ready, CountDownLatch start)
        throws InterruptedException {
        ready.countDown();
        start.await();
        userService.revokeWebSessions(userId);
        return null;
    }

    private int currentSessionVersion(UUID userId) {
        return userRepository.findSessionVersionById(userId).orElseThrow();
    }

    private UUID createUser() {
        String suffix = UUID.randomUUID().toString();
        return userService.createUser(
            "revoke-" + suffix.substring(0, 8),
            suffix + "@example.com",
            "old-hash",
            "Revocation User",
            AuthProvider.EMAIL
        ).getId();
    }
}
