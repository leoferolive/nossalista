package br.com.leoferolive.nossalista.langflowembed.dto;

import java.util.List;

/**
 * Corpo de {@code GET /.well-known/jwks.json} (RFC 7517 §5).
 */
public record LangflowJwksResponse(List<LangflowRsaJwk> keys) {
}
