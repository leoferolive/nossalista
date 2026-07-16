package br.com.leoferolive.nossalista.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuração central de execução assíncrona para I/O externo de notificação
 * (broadcast de notificações por usuário, push web e e-mail transacional).
 *
 * <p>Essas operações não devem bloquear a thread da requisição nem prolongar
 * transações de banco abertas — ver {@code docs/plans/onda1-blindagem-core/T3-notificacoes-async-timeouts.md}
 * e {@code docs/DECISIONS.md} (D-028). O executor é <b>bounded</b> (pool e
 * fila com limites explícitos) para não esgotar memória/threads sob carga;
 * a política de rejeição {@link ThreadPoolExecutor.CallerRunsPolicy} aplica
 * backpressure — quando a fila satura, a tarefa roda na própria thread
 * chamadora em vez de ser descartada silenciosamente ou derrubar a request.
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    /** Nome do executor usado por {@code @Async} nas operações de notificação/push/e-mail. */
    public static final String ASYNC_EXECUTOR = "notificationAsyncExecutor";

    @Override
    @Bean(name = ASYNC_EXECUTOR)
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("async-notif-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        // Não chamar executor.initialize() aqui: como este método é @Bean e
        // ThreadPoolTaskExecutor implementa InitializingBean, o container já
        // invoca initialize() em afterPropertiesSet(). Chamar manualmente
        // criaria um segundo ThreadPoolExecutor e abandonaria o primeiro.
        return executor;
    }

    /**
     * Métodos {@code @Async} com retorno {@code void} não propagam exceção
     * para o chamador (não há como, a chamada já retornou). Este handler
     * garante que a falha seja ao menos logada, em vez de silenciosamente
     * perdida.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return this::logUncaughtAsyncException;
    }

    private void logUncaughtAsyncException(Throwable ex, Method method, Object... params) {
        log.error("Falha não tratada em execução assíncrona: method={}", method.getName(), ex);
    }
}
