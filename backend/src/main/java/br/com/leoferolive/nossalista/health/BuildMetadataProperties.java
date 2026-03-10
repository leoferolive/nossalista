package br.com.leoferolive.nossalista.health;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Metadados de build/deploy expostos para auditoria operacional.
 */
@ConfigurationProperties(prefix = "app.build")
public record BuildMetadataProperties(
    String version,
    String gitSha,
    String gitTag,
    String environment,
    String buildTime
) {
}
