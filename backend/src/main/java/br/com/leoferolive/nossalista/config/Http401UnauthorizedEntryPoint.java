package br.com.leoferolive.nossalista.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

/**
 * Authentication Entry Point que retorna erro 401 no formato RFC 7807 Problem Details
 * para APIs REST quando usuário não autenticado tenta acessar endpoint protegido
 */
@Component
public class Http401UnauthorizedEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) throws IOException {
        // Criar ProblemDetail RFC 7807
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED,
            "Autenticação necessária para acessar este recurso"
        );
        problem.setType(URI.create("https://api.nossalista.com/docs/errors/not-authenticated"));
        problem.setTitle("Não autenticado");
        problem.setInstance(URI.create(request.getRequestURI()));

        // Configurar response
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        // Escrever JSON no response
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
