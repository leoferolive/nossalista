package br.com.leoferolive.nossalista.config;

import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do mapeamento de conflito de lock otimista para 409 RFC 7807
 * no {@link GlobalExceptionHandler}. Complementa os testes de concorrência de
 * {@code ListItemOptimisticLockingTest} e {@code SharedListOptimisticLockingTest}
 * (que provam que a exceção é lançada) verificando que a API responde no
 * formato correto quando ela chega ao handler global.
 */
class GlobalExceptionHandlerOptimisticLockingTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("ObjectOptimisticLockingFailureException responde 409 Conflict em formato RFC 7807")
    void handlesObjectOptimisticLockingFailureAs409() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/lists/123/items/456");
        ObjectOptimisticLockingFailureException ex =
            new ObjectOptimisticLockingFailureException("list_items", "456");

        ResponseEntity<ProblemDetail> response = handler.handleOptimisticLockingFailure(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(body.getDetail()).contains("alterado por outra pessoa");
        assertThat(body.getInstance()).hasToString("/api/lists/123/items/456");
    }

    @Test
    @DisplayName("jakarta.persistence.OptimisticLockException também responde 409 Conflict em formato RFC 7807")
    void handlesJakartaOptimisticLockExceptionAs409() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/lists/123");
        OptimisticLockException ex = new OptimisticLockException("stale entity");

        ResponseEntity<ProblemDetail> response = handler.handleOptimisticLockingFailure(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    }
}
