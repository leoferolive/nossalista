package br.com.leoferolive.nossalista.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

/**
 * Access Denied Handler que retorna erro 403 no formato RFC 7807 Problem Details
 * para APIs REST quando um usuário autenticado, porém sem a authority necessária
 * (ex.: ROLE_ADMIN exigida por {@code @PreAuthorize("hasRole('ADMIN')")}),
 * tenta acessar um recurso protegido.
 */
@Component
public class Http403AccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException accessDeniedException
    ) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN,
            "Você não tem permissão para acessar este recurso"
        );
        problem.setType(URI.create("https://api.nossalista.com/docs/errors/access-denied"));
        problem.setTitle("Acesso negado");
        problem.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        objectMapper.writeValue(response.getWriter(), problem);
    }
}
