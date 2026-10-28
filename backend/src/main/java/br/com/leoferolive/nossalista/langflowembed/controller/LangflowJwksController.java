package br.com.leoferolive.nossalista.langflowembed.controller;

import br.com.leoferolive.nossalista.langflowembed.config.LangflowEmbedProperties;
import br.com.leoferolive.nossalista.langflowembed.dto.LangflowJwksResponse;
import br.com.leoferolive.nossalista.langflowembed.service.LangflowEmbedKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Publica a chave pública do embed (JWKS) para o Langflow validar os tokens.
 */
@RestController
@Tag(name = "Langflow", description = "Embed do Langflow (spike)")
public class LangflowJwksController {

    private final LangflowEmbedProperties properties;
    private final LangflowEmbedKeyService keyService;

    public LangflowJwksController(LangflowEmbedProperties properties, LangflowEmbedKeyService keyService) {
        this.properties = properties;
        this.keyService = keyService;
    }

    @GetMapping("/.well-known/jwks.json")
    @Operation(summary = "Chave pública do embed do Langflow (JWKS)")
    public ResponseEntity<LangflowJwksResponse> jwks() {
        if (!properties.isEnabled()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(keyService.jwks());
    }
}
