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
    @DisplayName("requireBatchSizeWithinLimit lança InvalidInputException acima do teto, em inglês, sugerindo dividir em lotes")
    void requireBatchSizeWithinLimitThrowsAboveLimit() {
        assertThatThrownBy(() -> McpLimits.requireBatchSizeWithinLimit(McpLimits.MAX_BATCH_SIZE + 1, "items"))
            .isInstanceOf(InvalidInputException.class)
            .hasMessageContaining("items")
            .hasMessageContaining(String.valueOf(McpLimits.MAX_BATCH_SIZE))
            .hasMessageContaining("Split the request into multiple batches");
    }

    @Test
    @DisplayName("requirePageSizeWithinLimit usa o default quando nulo ou não-positivo")
    void requirePageSizeWithinLimitUsesDefaultWhenAbsent() {
        assertThat(McpLimits.requirePageSizeWithinLimit(null, 50, 500, "size")).isEqualTo(50);
        assertThat(McpLimits.requirePageSizeWithinLimit(0, 50, 500, "size")).isEqualTo(50);
        assertThat(McpLimits.requirePageSizeWithinLimit(-5, 50, 500, "size")).isEqualTo(50);
    }

    @Test
    @DisplayName("requirePageSizeWithinLimit retorna o valor pedido quando dentro do teto")
    void requirePageSizeWithinLimitReturnsRequestedWhenWithinLimit() {
        assertThat(McpLimits.requirePageSizeWithinLimit(200, 50, 500, "size")).isEqualTo(200);
        assertThat(McpLimits.requirePageSizeWithinLimit(500, 50, 500, "size")).isEqualTo(500);
    }

    @Test
    @DisplayName("requirePageSizeWithinLimit lança InvalidInputException acima do teto informado, em inglês")
    void requirePageSizeWithinLimitThrowsAboveLimit() {
        assertThatThrownBy(
            () -> McpLimits.requirePageSizeWithinLimit(101, 50, McpLimits.MAX_ACTIVITY_PAGE_SIZE, "size"))
            .isInstanceOf(InvalidInputException.class)
            .hasMessageContaining("size")
            .hasMessageContaining(String.valueOf(McpLimits.MAX_ACTIVITY_PAGE_SIZE))
            .hasMessageContaining("pagination");
    }

    @Test
    @DisplayName("requirePageSizeWithinLimit respeita tetos diferentes por chamador (get_list vs get_list_activity)")
    void requirePageSizeWithinLimitSupportsDifferentCapsPerCaller() {
        assertThatNoException().isThrownBy(
            () -> McpLimits.requirePageSizeWithinLimit(500, 100, McpLimits.MAX_LIST_ITEMS_PAGE_SIZE, "limit"));
        assertThatThrownBy(
            () -> McpLimits.requirePageSizeWithinLimit(101, 50, McpLimits.MAX_ACTIVITY_PAGE_SIZE, "size"))
            .isInstanceOf(InvalidInputException.class);
    }
}
