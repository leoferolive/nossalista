package br.com.leoferolive.nossalista.mcpoauth.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Valida PKCE (RFC 7636) com o método {@code S256} — o único aceito neste
 * servidor. {@code plain} é rejeitado explicitamente na criação do pedido de
 * autorização (nunca chega a ser validado aqui), conforme exigência OAuth 2.1.
 */
@Component
public class PkceValidator {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    /**
     * @param codeVerifier  o {@code code_verifier} enviado em {@code POST /oauth/token}
     * @param codeChallenge o {@code code_challenge} (S256) gerado no {@code GET /oauth/authorize}
     * @return true se {@code BASE64URL(SHA256(code_verifier)) == code_challenge}
     */
    public boolean matches(String codeVerifier, String codeChallenge) {
        if (codeVerifier == null || codeChallenge == null) {
            return false;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            String computed = URL_ENCODER.encodeToString(hash);
            return MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.US_ASCII),
                codeChallenge.getBytes(StandardCharsets.US_ASCII));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível na JVM", e);
        }
    }
}
