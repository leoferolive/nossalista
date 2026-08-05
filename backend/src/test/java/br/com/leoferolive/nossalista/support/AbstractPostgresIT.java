package br.com.leoferolive.nossalista.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class opt-in para testes de integração que precisam de um PostgreSQL
 * real (Testcontainers), em vez do H2 (MODE=PostgreSQL) que é o default da
 * suíte — ver {@code docs/plans/onda2-honestidade-metrica/T1-testcontainers.md}
 * e D-031 em {@code docs/DECISIONS.md}.
 *
 * <p><b>Aditivo:</b> só as classes que estendem esta base sobem o container.
 * O resto da suíte continua em H2 ({@code src/test/resources/application.yml}),
 * inalterado.</p>
 *
 * <p><b>Container singleton:</b> o campo estático é iniciado uma única vez, no
 * bloco {@code static}, e nunca é parado explicitamente — é o padrão
 * "Singleton Container" documentado pelo Testcontainers. Como o campo vive
 * nesta superclasse, é compartilhado por TODAS as subclasses executadas na
 * mesma JVM: o container sobe uma vez por execução de {@code mvn test} (não
 * uma vez por classe), e o Ryuk do Testcontainers (ou o Docker daemon) cuida
 * do encerramento ao fim da JVM.</p>
 *
 * <p>{@code @ServiceConnection} autoconfigura o {@code DataSource} do Spring
 * Boot para apontar para o container assim que uma classe de teste que
 * estende {@code AbstractPostgresIT} sobe o {@code ApplicationContext} —
 * o Flyway roda as 17+ migrations reais contra este Postgres, não contra o
 * dialeto de compatibilidade do H2.</p>
 *
 * <p><b>Isolamento entre testes:</b> como o container (e o schema) é
 * compartilhado entre todas as classes que estendem esta base, cada teste
 * migrado precisa garantir que não vaza estado: testes {@code @Transactional}
 * já fazem rollback automático ao final de cada método: nenhuma linha
 * sobrevive além do teste. Testes que não podem ser {@code @Transactional}
 * (ex.: os de lock otimista, que precisam de commits reais em transações
 * separadas para provar a corrida) limpam explicitamente o que criaram em
 * {@code @AfterEach}/{@code @AfterAll}.</p>
 */
public abstract class AbstractPostgresIT {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:17-alpine");

    @ServiceConnection
    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGRES_IMAGE);

    static {
        POSTGRES.start();
    }

    /**
     * O {@code src/test/resources/application.yml} global (H2, não editável por esta
     * tarefa) fixa {@code spring.jpa.database-platform=org.hibernate.dialect.H2Dialect}.
     * O {@code @ServiceConnection} troca o DataSource para o container Postgres, mas NÃO
     * mexe no dialeto do JPA — então o Hibernate acabaria falando H2Dialect com um banco
     * Postgres (ex.: {@code H2Dialect} anuncia suporte a {@code Connection.createClob()},
     * que o driver do Postgres não implementa, marcando a conexão do pool como "broken").
     * Aqui alinhamos o dialeto ao mesmo usado em dev/prod ({@code PostgreSQLDialect}).
     * {@code @DynamicPropertySource} tem precedência sobre o {@code application.yml}.
     */
    @DynamicPropertySource
    static void alignHibernateDialectToPostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }
}
