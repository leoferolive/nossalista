package br.com.leoferolive.nossalista.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Encaminha rotas de frontend (React Router) para index.html.
 * Exclui caminhos de backend e arquivos estáticos com extensão.
 *
 * <p>{@code mcp} é excluído porque o servidor MCP (Streamable HTTP) registra
 * seu próprio {@code RouterFunction} em {@code /mcp} — sem essa exclusão, o
 * mapeamento {@code @GetMapping} deste controller casa a rota antes do
 * router do MCP, e qualquer requisição {@code POST /mcp} recebe 405 em vez
 * de chegar ao transporte MCP (ver docs/DECISIONS.md D-020).</p>
 */
@Controller
public class SpaController {

    @GetMapping(value = {
        "/",
        "/{path:^(?!api|ws|actuator|v3|swagger-ui|assets|mcp)[^\\.]*}",
        "/{path:^(?!api|ws|actuator|v3|swagger-ui|assets|mcp)[^\\.]*}/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
