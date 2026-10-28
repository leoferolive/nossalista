package br.com.leoferolive.nossalista.langflowembed;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

/**
 * Chaves RSA geradas em memória para os testes do embed. Nunca são gravadas em disco.
 *
 * <p>{@link #publicKeyFromJwk} decodifica o JWK com código independente do de produção,
 * para que o teste valide o JWKS como um consumidor real (o Langflow) faria.</p>
 */
public final class LangflowEmbedTestKeys {

    private static final String PEM_HEADER = "-----BEGIN " + "PRIVATE KEY-----";
    private static final String PEM_FOOTER = "-----END " + "PRIVATE KEY-----";

    private LangflowEmbedTestKeys() {
    }

    public static KeyPair generate(int bits) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(bits);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA indisponível na JVM de teste", e);
        }
    }

    /**
     * Exemplo: {@code String pem = LangflowEmbedTestKeys.pkcs8Pem(keys.getPrivate());}
     */
    public static String pkcs8Pem(PrivateKey privateKey) {
        String body = Base64.getMimeEncoder(64, "\n".getBytes())
            .encodeToString(privateKey.getEncoded());
        return PEM_HEADER + "\n" + body + "\n" + PEM_FOOTER + "\n";
    }

    /**
     * Reconstrói a chave pública a partir de {@code n} e {@code e} do JWK, sem usar o código de produção.
     */
    public static RSAPublicKey publicKeyFromJwk(String n, String e) {
        try {
            BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(n));
            BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(e));
            PublicKey key = KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));
            return (RSAPublicKey) key;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | ClassCastException ex) {
            throw new IllegalStateException("JWK de teste inválido", ex);
        }
    }
}
