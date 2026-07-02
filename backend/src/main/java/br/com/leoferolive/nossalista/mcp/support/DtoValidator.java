package br.com.leoferolive.nossalista.mcp.support;

import br.com.leoferolive.nossalista.common.exception.ValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Aplica Bean Validation (as mesmas anotações {@code jakarta.validation} usadas
 * nos DTOs REST, ex.: {@code CreateListRequest}, {@code CreateItemRequestDTO})
 * fora do ciclo de vida do Spring MVC. As tools MCP chamam os services
 * diretamente, sem passar pelo {@code @Valid} de um controller, então essa
 * validação precisa ser disparada manualmente antes de repassar o DTO ao service.
 */
@Component
public class DtoValidator {

    private final Validator validator;

    public DtoValidator(Validator validator) {
        this.validator = validator;
    }

    /**
     * Valida todas as constraints declaradas no objeto. Use para DTOs onde
     * todo campo é sempre obrigatório/validado (ex.: criação de recursos).
     *
     * @throws ValidationException com as mensagens de todas as violações encontradas
     */
    public <T> void validate(T dto) {
        failIfAny(validator.validate(dto));
    }

    /**
     * Valida apenas a propriedade informada do objeto já preenchido. Use para
     * DTOs de atualização parcial (semântica PATCH), onde só os campos
     * efetivamente fornecidos pelo modelo devem ser checados — validar o
     * objeto inteiro disparasia constraints como {@code @NotBlank} em campos
     * que o usuário deliberadamente deixou de fora da chamada.
     *
     * @throws ValidationException com as mensagens das violações da propriedade
     */
    public <T> void validateProperty(T dto, String propertyName) {
        failIfAny(validator.validateProperty(dto, propertyName));
    }

    private <T> void failIfAny(Set<ConstraintViolation<T>> violations) {
        if (violations.isEmpty()) {
            return;
        }
        String message = violations.stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.joining("; "));
        throw new ValidationException(message);
    }
}
