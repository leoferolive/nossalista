package br.com.leoferolive.nossalista.langflowembed.dto;

import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * Converte a chave pública RSA no JWKS publicado ao Langflow.
 */
public final class LangflowJwksMapper {

    private LangflowJwksMapper() {
    }

    /**
     * Monta o JWKS com uma única chave de assinatura RS256.
     *
     * <p>Exemplo: {@code LangflowJwksMapper.toJwks("nl-langflow-1", publicKey)}</p>
     *
     * @param keyId identificador da chave, igual ao header {@code kid} dos tokens
     * @param publicKey chave pública RSA
     * @return JWKS com {@code kty=RSA}, {@code use=sig} e {@code alg=RS256}
     */
    public static LangflowJwksResponse toJwks(String keyId, RSAPublicKey publicKey) {
        LangflowRsaJwk jwk = new LangflowRsaJwk(
            "RSA",
            "sig",
            "RS256",
            keyId,
            unsignedBase64Url(publicKey.getModulus()),
            unsignedBase64Url(publicKey.getPublicExponent())
        );
        return new LangflowJwksResponse(List.of(jwk));
    }

    /**
     * Codifica o valor sem o byte de sinal que {@link BigInteger#toByteArray()} pode
     * acrescentar (RFC 7518 §6.3.1: o inteiro é big-endian sem zeros à esquerda).
     */
    private static String unsignedBase64Url(BigInteger value) {
        byte[] bytes = value.toByteArray();
        boolean hasSignByte = bytes.length > 1 && bytes[0] == 0;
        byte[] magnitude = hasSignByte ? Arrays.copyOfRange(bytes, 1, bytes.length) : bytes;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(magnitude);
    }
}
