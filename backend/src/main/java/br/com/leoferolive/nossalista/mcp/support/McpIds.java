package br.com.leoferolive.nossalista.mcp.support;

import br.com.leoferolive.nossalista.common.exception.InvalidInputException;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/**
 * Parsing de identificadores e datas recebidos como texto pelas tools MCP.
 * Todo parâmetro de tipo id/data das tools é declarado como {@code String}
 * (nunca {@code UUID}/{@code LocalDateTime} diretos) para que uma entrada
 * malformada vire uma {@link InvalidInputException} com mensagem acionável,
 * em vez de uma exceção técnica de desserialização do framework MCP.
 */
public final class McpIds {

    private McpIds() {
    }

    public static UUID parseUuid(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidInputException(fieldName + " is required");
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new InvalidInputException(
                fieldName + " is invalid: it must be a valid UUID (received \"" + value + "\")");
        }
    }

    /**
     * @return o {@link LocalDateTime} parseado, ou {@code null} se value for nulo/em branco
     */
    public static LocalDateTime parseDateTime(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new InvalidInputException(
                fieldName + " is invalid: use ISO-8601 format (e.g. 2026-12-31T23:59:00). Received \""
                    + value + "\"");
        }
    }
}
