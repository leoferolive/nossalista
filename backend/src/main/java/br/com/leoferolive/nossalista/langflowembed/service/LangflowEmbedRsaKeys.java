package br.com.leoferolive.nossalista.langflowembed.service;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Par de chaves RSA do embed: a privada assina o token, a pública vai ao JWKS.
 */
record LangflowEmbedRsaKeys(RSAPrivateKey privateKey, RSAPublicKey publicKey) {
}
