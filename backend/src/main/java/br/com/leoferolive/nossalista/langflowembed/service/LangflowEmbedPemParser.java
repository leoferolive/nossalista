package br.com.leoferolive.nossalista.langflowembed.service;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

/**
 * Lê a chave privada RSA do embed a partir de PEM PKCS#8 e deriva a pública.
 *
 * <p>Erros nunca ecoam o conteúdo recebido: a chave é segredo, então a mensagem
 * descreve o formato esperado em vez do valor (ver convenções em CLAUDE.md).</p>
 */
final class LangflowEmbedPemParser {

    /** Mínimo para RS256; abaixo disso a assinatura é fraca demais para um token de sessão. */
    static final int MIN_MODULUS_BITS = 2048;

    private static final String PKCS8_BEGIN = "-----BEGIN " + "PRIVATE KEY-----";
    private static final String PKCS8_END = "-----END " + "PRIVATE KEY-----";
    private static final String PKCS1_BEGIN = "-----BEGIN " + "RSA PRIVATE KEY-----";

    private LangflowEmbedPemParser() {
    }

    /**
     * Converte o PEM em par de chaves RSA. Aceita {@code \n} escapado literalmente,
     * comum em variáveis de ambiente de k8s e CI.
     *
     * <p>Exemplo: {@code LangflowEmbedRsaKeys keys = parse(pemFromEnv);}</p>
     *
     * @param pem conteúdo PEM PKCS#8 da chave privada
     * @return chave privada e pública RSA com módulo de pelo menos 2048 bits
     * @throws IllegalStateException se o PEM não for PKCS#8 RSA válido ou a chave for fraca
     */
    static LangflowEmbedRsaKeys parse(String pem) {
        String normalized = pem.replace("\\n", "\n");
        rejectPkcs1(normalized);
        RSAPrivateCrtKey privateKey = decodePkcs8(normalized);
        checkModulusSize(privateKey.getModulus());
        return new LangflowEmbedRsaKeys(privateKey, derivePublicKey(privateKey));
    }

    private static void rejectPkcs1(String pem) {
        if (pem.contains(PKCS1_BEGIN)) {
            throw new IllegalStateException(
                "chave do Langflow em PKCS#1 (BEGIN RSA PRIVATE KEY) não suportada: "
                    + "converta para PKCS#8 (BEGIN PRIVATE KEY), por exemplo com "
                    + "'openssl pkcs8 -topk8 -nocrypt'");
        }
    }

    private static RSAPrivateCrtKey decodePkcs8(String pem) {
        if (!pem.contains(PKCS8_BEGIN) || !pem.contains(PKCS8_END)) {
            throw new IllegalStateException(
                "chave do Langflow inválida: esperado PEM PKCS#8 com "
                    + "'" + PKCS8_BEGIN + "' e '" + PKCS8_END + "'");
        }
        try {
            String body = pem.replace(PKCS8_BEGIN, "").replace(PKCS8_END, "");
            byte[] der = Base64.getMimeDecoder().decode(body);
            return asCrtKey(KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der)));
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            throw new IllegalStateException(
                "chave do Langflow inválida: PEM PKCS#8 RSA não pôde ser decodificado", e);
        }
    }

    private static RSAPrivateCrtKey asCrtKey(PrivateKey key) {
        if (key instanceof RSAPrivateCrtKey crtKey) {
            return crtKey;
        }
        throw new IllegalStateException("chave do Langflow inválida: esperado chave RSA com CRT");
    }

    private static void checkModulusSize(BigInteger modulus) {
        if (modulus.bitLength() < MIN_MODULUS_BITS) {
            throw new IllegalStateException(
                "chave do Langflow curta demais: " + modulus.bitLength() + " bits "
                    + "(mínimo " + MIN_MODULUS_BITS + " bits para RS256)");
        }
    }

    private static RSAPublicKey derivePublicKey(RSAPrivateCrtKey privateKey) {
        try {
            RSAPublicKeySpec spec = new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent());
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("não foi possível derivar a chave pública do Langflow", e);
        }
    }
}
