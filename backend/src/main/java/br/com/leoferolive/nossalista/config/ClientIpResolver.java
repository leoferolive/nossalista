package br.com.leoferolive.nossalista.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Resolve o IP real do cliente a partir de um header confiável definido pela borda.
 *
 * <p>Topologia de rede: {@code Cloudflare Tunnel -> Traefik (K3s ingress) -> pod Spring Boot}.
 * O Cloudflare reescreve o header {@code CF-Connecting-IP} com o IP real do cliente e este valor
 * <strong>não pode ser spoofado</strong> pelo cliente — qualquer valor enviado pelo cliente é
 * sobrescrito pela borda Cloudflare.
 *
 * <p>O header {@code X-Forwarded-For} é deliberadamente ignorado porque é controlado pelo cliente:
 * confiar nele permitiria a um atacante enviar um valor diferente a cada requisição e burlar
 * qualquer rate limit ancorado no IP (ex.: brute-force de token em {@code reset-password}).
 *
 * <p>Quando nenhum header confiável está presente (ex.: chamada interna direta ao pod, sem passar
 * pela borda), faz fallback seguro para {@link HttpServletRequest#getRemoteAddr()}, que nunca quebra.
 */
@Component
public class ClientIpResolver {

    /** Header confiável injetado pela Cloudflare com o IP real do cliente. */
    static final String TRUSTED_CLIENT_IP_HEADER = "CF-Connecting-IP";

    /**
     * Resolve o IP do cliente para uso como chave de rate limiting.
     *
     * @param request requisição HTTP corrente
     * @return IP do header confiável da borda quando presente; caso contrário, o IP remoto da conexão
     */
    public String resolve(HttpServletRequest request) {
        String trustedClientIp = request.getHeader(TRUSTED_CLIENT_IP_HEADER);
        if (trustedClientIp != null && !trustedClientIp.isBlank()) {
            return trustedClientIp.trim();
        }
        return request.getRemoteAddr();
    }
}
