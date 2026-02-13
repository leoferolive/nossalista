package br.com.leoferolive.nossalista.config;

import br.com.leoferolive.nossalista.auth.exception.EmailAlreadyExistsException;
import br.com.leoferolive.nossalista.auth.exception.InvalidCredentialsException;
import br.com.leoferolive.nossalista.auth.exception.UsernameAlreadyExistsException;
import br.com.leoferolive.nossalista.common.exception.ForbiddenException;
import br.com.leoferolive.nossalista.common.exception.ValidationException;
import br.com.leoferolive.nossalista.list.exception.InvalidListTypeException;
import br.com.leoferolive.nossalista.list.exception.InviteCodeGenerationException;
import br.com.leoferolive.nossalista.list.exception.ListNotFoundException;
import br.com.leoferolive.nossalista.user.exception.NotAuthenticatedException;
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
     * Trata exceção de credenciais inválidas (email não existe OU senha incorreta)
     * Retorna 401 Unauthorized com RFC 7807 Problem Details
     * Mensagem genérica por segurança: não revela se email existe
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCredentials(
        InvalidCredentialsException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED,
            ex.getMessage()
        );
        problem.setType(URI.create("https://api.nossalista.com/docs/errors/invalid-credentials"));
        problem.setTitle("Credenciais inválidas");
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    /**
     * Trata exceção de tipo de lista inválido
     * Retorna 400 Bad Request com RFC 7807 Problem Details
     */
    @ExceptionHandler(InvalidListTypeException.class)
    public ResponseEntity<ProblemDetail> handleInvalidListType(
        InvalidListTypeException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.getMessage()
        );
        problem.setType(URI.create("https://api.nossalista.com/docs/errors/invalid-list-type"));
        problem.setTitle("Tipo de lista inválido");
        problem.setProperty("invalidTypeId", ex.getInvalidTypeId());
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.badRequest().body(problem);
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

    /**
     * Trata exceção de argumento ilegal (validação manual)
     * Retorna 400 Bad Request com RFC 7807 Problem Details
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(
        IllegalArgumentException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.getMessage()
        );
        problem.setType(URI.create("https://api.nossalista.com/docs/errors/validation-error"));
        problem.setTitle("Validation Error");
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.badRequest().body(problem);
    }

    /**
     * Trata exceção de validação de regras de negócio
     * Retorna 400 Bad Request com RFC 7807 Problem Details
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
        ValidationException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.getMessage()
        );
        problem.setType(URI.create("https://api.nossalista.com/docs/errors/validation-error"));
        problem.setTitle("Erro de Validação");
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.badRequest().body(problem);
    }

    /**
     * Trata exceção de usuário não autenticado
     * Retorna 401 Unauthorized com RFC 7807 Problem Details
     */
    @ExceptionHandler(NotAuthenticatedException.class)
    public ResponseEntity<ProblemDetail> handleNotAuthenticated(
        NotAuthenticatedException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED,
            ex.getMessage()
        );
        problem.setType(URI.create("https://api.nossalista.com/docs/errors/not-authenticated"));
        problem.setTitle("Não autenticado");
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    /**
     * Trata exceção de falha na geração de código de convite
     * Retorna 500 Internal Server Error com RFC 7807 Problem Details
     */
    @ExceptionHandler(InviteCodeGenerationException.class)
    public ResponseEntity<ProblemDetail> handleInviteCodeGeneration(
        InviteCodeGenerationException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ex.getMessage()
        );
        problem.setType(URI.create("https://api.nossalista.com/docs/errors/invite-code-generation-failed"));
        problem.setTitle("Falha ao gerar código de convite");
        problem.setProperty("maxAttempts", ex.getMaxAttempts());
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    /**
     * Trata exceção de lista não encontrada
     * Retorna 404 Not Found com RFC 7807 Problem Details
     */
    @ExceptionHandler(ListNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleListNotFound(
        ListNotFoundException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            ex.getMessage()
        );
        problem.setType(URI.create("https://api.nossalista.com/docs/errors/list-not-found"));
        problem.setTitle("Lista Não Encontrada");
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    /**
     * Trata exceção de acesso proibido (usuário sem permissão)
     * Retorna 403 Forbidden com RFC 7807 Problem Details
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ProblemDetail> handleForbidden(
        ForbiddenException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN,
            ex.getMessage()
        );
        problem.setType(URI.create("https://api.nossalista.com/docs/errors/access-forbidden"));
        problem.setTitle("Acesso Negado");
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }
}
