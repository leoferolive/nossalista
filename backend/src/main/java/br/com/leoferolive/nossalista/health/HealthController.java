package br.com.leoferolive.nossalista.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller para verificação de saúde da aplicação.
 * Útil para health checks do Kubernetes e testes básicos.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final BuildMetadataProperties buildMetadata;

    public HealthController(BuildMetadataProperties buildMetadata) {
        this.buildMetadata = buildMetadata;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("application", "NossaLista API");
        response.put("version", buildMetadata.version());
        response.put("gitSha", buildMetadata.gitSha());
        response.put("gitTag", buildMetadata.gitTag());
        response.put("environment", buildMetadata.environment());
        response.put("buildTime", buildMetadata.buildTime());

        return ResponseEntity.ok(response);
    }
}
