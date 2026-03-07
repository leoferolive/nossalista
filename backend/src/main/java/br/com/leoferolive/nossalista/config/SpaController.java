package br.com.leoferolive.nossalista.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Encaminha rotas de frontend (React Router) para index.html.
 * Exclui caminhos de backend e arquivos estáticos com extensão.
 */
@Controller
public class SpaController {

    @GetMapping(value = {
        "/",
        "/{path:^(?!api|ws|actuator|v3|swagger-ui)[^\\.]*}",
        "/{path:^(?!api|ws|actuator|v3|swagger-ui)[^\\.]*}/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
