package br.com.leoferolive.nossalista.config;

import br.com.leoferolive.nossalista.auth.exception.EmailAlreadyExistsException;
import br.com.leoferolive.nossalista.auth.exception.UsernameAlreadyExistsException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler global de exceções seguindo o padrão RFC 7807 Problem Details
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Trata exceção de email já existente
     * Retorna 409 Conflict com RFC 7807 Problem Details
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleEmailAlreadyExists(
        EmailAlreadyExistsException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            ex.getMessage()
        );
        problem.setType(URI.create("https://api.nossalista.com/docs/errors/email-already-exists"));
        problem.setTitle("Email já cadastrado");
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    /**
     * Trata exceção de username já existente
     * Retorna 409 Conflict com RFC 7807 Problem Details
     */
    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleUsernameAlreadyExists(
        UsernameAlreadyExistsException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            ex.getMessage()
        );
        problem.setType(URI.create("https://api.nossalista.com/docs/errors/username-already-exists"));
        problem.setTitle("Username já cadastrado");
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    /**
     * Trata erros de validação da anotação @Valid
     * Retorna 400 Bad Request com RFC 7807 Problem Details incluindo erros de campo
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationErrors(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Erro de validação nos dados fornecidos"
        );
        problem.setType(URI.create("https://api.nossalista.com/docs/errors/validation-error"));
        problem.setTitle("Validation Error");
        problem.setProperty("errors", errors);
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.badRequest().body(problem);
    }
}
