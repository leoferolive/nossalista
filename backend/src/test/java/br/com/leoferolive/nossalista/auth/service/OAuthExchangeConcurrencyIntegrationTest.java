package br.com.leoferolive.nossalista.auth.service;

import br.com.leoferolive.nossalista.auth.domain.OAuthAuthorizationCode;
import br.com.leoferolive.nossalista.auth.exception.InvalidOAuthCodeException;
import br.com.leoferolive.nossalista.auth.repository.OAuthAuthorizationCodeRepository;
import br.com.leoferolive.nossalista.user.domain.AuthProvider;
import br.com.leoferolive.nossalista.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** Exercises one code redemption through independent concurrent transactions. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@DisplayName("OAuth exchange concurrent redemption")
class OAuthExchangeConcurrencyIntegrationTest {

    private static final int REDEEMERS = 12;

    @Autowired
    private OAuthCodeStore codeStore;

    @Autowired
    private OAuthExchangeService exchangeService;

    @Autowired
    private OAuthAuthorizationCodeRepository codeRepository;

    @Autowired
    private UserService userService;

    private ExecutorService executor;

    @AfterEach
    void stopExecutor() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    void exactlyOneConcurrentRedemptionSucceeds() throws Exception {
        UUID userId = createUser();
        String code = codeStore.issue(userId);
        executor = Executors.newFixedThreadPool(REDEEMERS);
        CountDownLatch ready = new CountDownLatch(REDEEMERS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> attempts = new ArrayList<>();

        for (int i = 0; i < REDEEMERS; i++) {
            attempts.add(executor.submit(() -> redeem(code, ready, start)));
        }

        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        long successes = countSuccesses(attempts);

        assertThat(successes).isEqualTo(1);
        OAuthAuthorizationCode claimed = codeRepository.findByCodeHash(hash(code)).orElseThrow();
        assertThat(claimed.getConsumedAt()).isNotNull();
    }

    private UUID createUser() {
        String suffix = UUID.randomUUID().toString();
        return userService.createUser(
            "concurrent-" + suffix.substring(0, 8),
            suffix + "@example.com",
            "hash",
            "Concurrent OAuth User",
            AuthProvider.EMAIL
        ).getId();
    }

    private boolean redeem(String code, CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await(10, TimeUnit.SECONDS);
        try {
            exchangeService.exchange(code);
            return true;
        } catch (InvalidOAuthCodeException exception) {
            return false;
        }
    }

    private long countSuccesses(List<Future<Boolean>> attempts) throws InterruptedException, ExecutionException {
        long successes = 0;
        for (Future<Boolean> attempt : attempts) {
            if (attempt.get()) {
                successes++;
            }
        }
        return successes;
    }

    private String hash(String code) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(code.getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }
}
