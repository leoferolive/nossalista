package br.com.leoferolive.nossalista.langflowembed.dto;

/**
 * Chave pública RSA em JWK (RFC 7517 §6.3 e RFC 7518 §6.3.1).
 *
 * <p>{@code n} e {@code e} são o módulo e o expoente em base64url sem padding.</p>
 */
public record LangflowRsaJwk(String kty, String use, String alg, String kid, String n, String e) {
}
