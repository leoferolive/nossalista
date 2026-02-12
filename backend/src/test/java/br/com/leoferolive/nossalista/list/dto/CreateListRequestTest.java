package br.com.leoferolive.nossalista.list.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de validação para CreateListRequest DTO
 */
class CreateListRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Deve passar com dados válidos")
    void shouldPassWithValidData() {
        CreateListRequest request = new CreateListRequest("Mercado Semanal", 1);
        Set<ConstraintViolation<CreateListRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty(), "Deve ter zero violações com dados válidos");
    }

    @Test
    @DisplayName("Deve falhar quando nome é nulo")
    void shouldFailWhenNameIsNull() {
        CreateListRequest request = new CreateListRequest(null, 1);
        Set<ConstraintViolation<CreateListRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty(), "Deve ter violações quando nome é nulo");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Nome é obrigatório")));
    }

    @Test
    @DisplayName("Deve falhar quando nome é menor que 3 caracteres")
    void shouldFailWhenNameIsTooShort() {
        CreateListRequest request = new CreateListRequest("AB", 1);
        Set<ConstraintViolation<CreateListRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty(), "Deve ter violações quando nome < 3 caracteres");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("entre 3 e 100")));
    }

    @Test
    @DisplayName("Deve falhar quando nome é maior que 100 caracteres")
    void shouldFailWhenNameIsTooLong() {
        String longName = "A".repeat(101);
        CreateListRequest request = new CreateListRequest(longName, 1);
        Set<ConstraintViolation<CreateListRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty(), "Deve ter violações quando nome > 100 caracteres");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("entre 3 e 100")));
    }

    @Test
    @DisplayName("Deve falhar quando typeId é nulo")
    void shouldFailWhenTypeIdIsNull() {
        CreateListRequest request = new CreateListRequest("Lista Teste", null);
        Set<ConstraintViolation<CreateListRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty(), "Deve ter violações quando typeId é nulo");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Tipo de lista é obrigatório")));
    }

    @Test
    @DisplayName("Deve falhar quando typeId é menor que 1")
    void shouldFailWhenTypeIdIsLessThanOne() {
        CreateListRequest request = new CreateListRequest("Lista Teste", 0);
        Set<ConstraintViolation<CreateListRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty(), "Deve ter violações quando typeId < 1");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Tipo de lista deve estar entre 1 e 4")));
    }
}
