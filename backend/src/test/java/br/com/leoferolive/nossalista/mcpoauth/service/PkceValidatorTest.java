package br.com.leoferolive.nossalista.mcpoauth.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class PkceValidatorTest {

    private final PkceValidator validator = new PkceValidator();

    private String s256(String verifier) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    @Test
    void matchesWhenChallengeIsSha256Base64UrlOfVerifier() throws Exception {
        String verifier = "a-valid-code-verifier-with-enough-entropy-1234567890";
        String challenge = s256(verifier);

        assertThat(validator.matches(verifier, challenge)).isTrue();
    }

    @Test
    void doesNotMatchWrongVerifier() throws Exception {
        String challenge = s256("correct-verifier");

        assertThat(validator.matches("wrong-verifier", challenge)).isFalse();
    }

    @Test
    void doesNotMatchWhenVerifierIsNull() {
        assertThat(validator.matches(null, "challenge")).isFalse();
    }

    @Test
    void doesNotMatchWhenChallengeIsNull() {
        assertThat(validator.matches("verifier", null)).isFalse();
    }

    @Test
    void doesNotMatchPlainVerifierAgainstItself() {
        // plain nunca é aceito por este servidor: o "challenge" plain (== verifier)
        // nunca bate contra o hash S256 calculado aqui.
        String verifier = "plain-style-challenge-equals-verifier";
        assertThat(validator.matches(verifier, verifier)).isFalse();
    }
}
