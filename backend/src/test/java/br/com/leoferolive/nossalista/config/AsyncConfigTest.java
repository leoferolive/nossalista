package br.com.leoferolive.nossalista.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AsyncConfig Tests")
class AsyncConfigTest {

    private final AsyncConfig asyncConfig = new AsyncConfig();
    private ThreadPoolTaskExecutor executor;

    @BeforeEach
    void setUp() {
        executor = (ThreadPoolTaskExecutor) asyncConfig.getAsyncExecutor();
        // Fora do container Spring não há afterPropertiesSet() para inicializar
        // o pool; inicializamos manualmente aqui para poder inspecioná-lo.
        // Em produção quem chama initialize() é o próprio container (o método
        // getAsyncExecutor() deliberadamente não o faz — ver AsyncConfig).
        executor.initialize();
    }

    @AfterEach
    void tearDown() {
        // Evita vazar threads entre testes: cada chamada a getAsyncExecutor()
        // cria e inicializa um pool novo (fora do container Spring não há
        // ciclo de vida de bean gerenciando o shutdown).
        executor.shutdown();
    }

    @Test
    @DisplayName("Classe deve estar anotada com @EnableAsync")
    void shouldEnableAsync() {
        assertThat(AsyncConfig.class.isAnnotationPresent(EnableAsync.class)).isTrue();
    }

    @Test
    @DisplayName("Executor deve ser bounded (pool e fila com limites explícitos)")
    void executorShouldBeBounded() {
        assertThat(executor.getCorePoolSize()).isPositive();
        assertThat(executor.getMaxPoolSize()).isGreaterThanOrEqualTo(executor.getCorePoolSize());
        assertThat(executor.getQueueCapacity()).isPositive();
        assertThat(executor.getThreadNamePrefix()).isEqualTo("async-notif-");
    }

    @Test
    @DisplayName("Política de rejeição deve aplicar backpressure (CallerRunsPolicy)")
    void rejectionPolicyShouldApplyBackpressure() {
        assertThat(executor.getThreadPoolExecutor().getRejectedExecutionHandler())
                .isInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class);
    }

    @Test
    @DisplayName("AsyncUncaughtExceptionHandler não deve propagar exceção, apenas tratá-la")
    void uncaughtExceptionHandlerShouldNotThrow() throws NoSuchMethodException {
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        AtomicBoolean handled = new AtomicBoolean(false);

        handler.handleUncaughtException(
                new RuntimeException("erro assíncrono simulado"),
                AsyncConfigTest.class.getDeclaredMethod("uncaughtExceptionHandlerShouldNotThrow"));
        handled.set(true);

        assertThat(handled).isTrue();
    }
}
