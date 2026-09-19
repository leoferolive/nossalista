package br.com.leoferolive.nossalista.auth.service;

/**
 * Trusted Google OpenID Connect claims needed to bind an external identity.
 *
 * @param issuer provider issuer, which must be the configured canonical issuer
 * @param subject stable provider subject ({@code sub})
 * @param email provider e-mail claim
 * @param emailVerified provider assertion that the e-mail is verified
 * @param name current provider display name
 * @param picture current provider avatar URL
 */
public record GoogleIdentityClaims(
    String issuer,
    String subject,
    String email,
    boolean emailVerified,
    String name,
    String picture
) {
}
