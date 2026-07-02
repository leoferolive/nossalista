package br.com.leoferolive.nossalista.mcp.support;

import br.com.leoferolive.nossalista.common.exception.InvalidInputException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("McpLimits")
class McpLimitsTest {

    @Test
    @DisplayName("requireBatchSizeWithinLimit não lança dentro do teto (inclusive no limite exato)")
    void requireBatchSizeWithinLimitAllowsWithinLimit() {
        assertThatNoException()
            .isThrownBy(() -> McpLimits.requireBatchSizeWithinLimit(McpLimits.MAX_BATCH_SIZE, "items"));
    }

    @Test
    @DisplayName("requireBatchSizeWithinLimit lança InvalidInputException acima do teto")
    void requireBatchSizeWithinLimitThrowsAboveLimit() {
        assertThatThrownBy(() -> McpLimits.requireBatchSizeWithinLimit(McpLimits.MAX_BATCH_SIZE + 1, "items"))
            .isInstanceOf(InvalidInputException.class)
            .hasMessageContaining("items")
            .hasMessageContaining(String.valueOf(McpLimits.MAX_BATCH_SIZE));
    }

    @Test
    @DisplayName("requirePageSizeWithinLimit usa o default quando nulo ou não-positivo")
    void requirePageSizeWithinLimitUsesDefaultWhenAbsent() {
        assertThat(McpLimits.requirePageSizeWithinLimit(null, 50, "size")).isEqualTo(50);
        assertThat(McpLimits.requirePageSizeWithinLimit(0, 50, "size")).isEqualTo(50);
        assertThat(McpLimits.requirePageSizeWithinLimit(-5, 50, "size")).isEqualTo(50);
    }

    @Test
    @DisplayName("requirePageSizeWithinLimit retorna o valor pedido quando dentro do teto")
    void requirePageSizeWithinLimitReturnsRequestedWhenWithinLimit() {
        assertThat(McpLimits.requirePageSizeWithinLimit(200, 50, "size")).isEqualTo(200);
        assertThat(McpLimits.requirePageSizeWithinLimit(McpLimits.MAX_PAGE_SIZE, 50, "size"))
            .isEqualTo(McpLimits.MAX_PAGE_SIZE);
    }

    @Test
    @DisplayName("requirePageSizeWithinLimit lança InvalidInputException acima do teto")
    void requirePageSizeWithinLimitThrowsAboveLimit() {
        assertThatThrownBy(() -> McpLimits.requirePageSizeWithinLimit(McpLimits.MAX_PAGE_SIZE + 1, 50, "size"))
            .isInstanceOf(InvalidInputException.class)
            .hasMessageContaining("size")
            .hasMessageContaining(String.valueOf(McpLimits.MAX_PAGE_SIZE));
    }
}
