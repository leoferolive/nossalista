# Story 2.1: Modelagem de Dados de Listas e Tipos

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a desenvolvedor,
I want criar a estrutura de dados para listas e tipos,
So that o sistema possa armazenar e gerenciar listas pessoais.

## Acceptance Criteria

**Given** migração Flyway V2__create_list_types_and_lists.sql
**When** executada
**Then** tabela list_types deve ter colunas: id (SERIAL), name (VARCHAR), slug (VARCHAR, unique), created_at
**And** deve ter 4 tipos pré-inseridos: (1, "Compras", "compras"), (2, "Tarefas", "tarefas"), (3, "Wishlist", "wishlist"), (4, "Genérica", "generica")

**Given** migração V2 executada
**When** tabela lists é criada
**Then** tabela lists deve ter colunas: id (UUID), name, type_id (FK), owner_id (FK), invite_code, created_at, updated_at
**And** deve ter índices: idx_lists_owner_id, idx_lists_invite_code
**And** deve ter constraints: fk_lists_type, fk_lists_owner

**Given** entidade List no backend
**When** mapeada via JPA
**Then** deve ter @Entity com campos: id, name, type, owner, inviteCode, createdAt, updatedAt
**And** @PreUpdate para atualizar updated_at automaticamente

## Tasks / Subtasks

- [ ] Task 1: Criar migração Flyway V2__create_list_types_and_lists.sql (AC: Tabelas list_types e lists)
  - [ ] 1.1: Criar arquivo V2__create_list_types_and_lists.sql em src/main/resources/db/migration/
  - [ ] 1.2: Criar tabela list_types com id SERIAL, name VARCHAR(50), slug VARCHAR(50) UNIQUE, created_at TIMESTAMP
  - [ ] 1.3: INSERT 4 tipos: (1, 'Compras', 'compras'), (2, 'Tarefas', 'tarefas'), (3, 'Wishlist', 'wishlist'), (4, 'Genérica', 'generica')
  - [ ] 1.4: Criar tabela lists com id UUID PRIMARY KEY, name VARCHAR(100) NOT NULL, type_id INTEGER NOT NULL, owner_id UUID NOT NULL, invite_code VARCHAR(20) UNIQUE NULLABLE, created_at TIMESTAMP, updated_at TIMESTAMP
  - [ ] 1.5: Adicionar foreign key fk_lists_type (type_id) REFERENCES list_types(id)
  - [ ] 1.6: Adicionar foreign key fk_lists_owner (owner_id) REFERENCES users(id) ON DELETE CASCADE
  - [ ] 1.7: Criar índice idx_lists_owner_id ON lists(owner_id)
  - [ ] 1.8: Criar índice idx_lists_invite_code ON lists(invite_code)
  - [ ] 1.9: Adicionar DEFAULT NOW() para created_at e updated_at
  - [ ] 1.10: Testar migração localmente com PostgreSQL e H2

- [ ] Task 2: Criar enum ListType (AC: Enum para tipos de lista)
  - [ ] 2.1: Criar enum ListType em list/domain/ com valores: SHOPPING, TASK, WISHLIST, GENERIC
  - [ ] 2.2: Adicionar método String getSlug() que retorna slug lowercase (shopping, task, wishlist, generic)
  - [ ] 2.3: Adicionar método estático ListType fromSlug(String slug) para conversão
  - [ ] 2.4: Adicionar JavaDoc explicando mapeamento com list_types

- [ ] Task 3: Criar entidade ListTypeEntity (AC: Entidade para list_types)
  - [ ] 3.1: Criar ListTypeEntity em list/domain/ com @Entity(name="list_types")
  - [ ] 3.2: Adicionar campos: id (Integer), name (String), slug (String), createdAt (LocalDateTime)
  - [ ] 3.3: Adicionar @Id @GeneratedValue(strategy = GenerationType.IDENTITY) para id
  - [ ] 3.4: Adicionar @Column(unique=true) para slug
  - [ ] 3.5: Marcar entidade como @Immutable (tipos são fixos, não editáveis)

- [ ] Task 4: Criar entidade List (AC: Entidade JPA para lists)
  - [ ] 4.1: Criar List entity em list/domain/ com @Entity(name="lists")
  - [ ] 4.2: Adicionar campos: id (UUID), name (String), typeId (Integer), ownerId (UUID), inviteCode (String), createdAt (LocalDateTime), updatedAt (LocalDateTime)
  - [ ] 4.3: Adicionar @Id @GeneratedValue(generator = "UUID") para id
  - [ ] 4.4: Adicionar @ManyToOne relacionamento com User (owner) usando owner_id
  - [ ] 4.5: Adicionar @ManyToOne relacionamento com ListTypeEntity usando type_id
  - [ ] 4.6: Adicionar @Column(unique=true) para inviteCode
  - [ ] 4.7: Adicionar @PreUpdate para atualizar updatedAt automaticamente
  - [ ] 4.8: Implementar método helper getType() que retorna ListType baseado em typeId
  - [ ] 4.9: Adicionar validações: @NotBlank para name, @Size(min=3, max=100) para name

- [ ] Task 5: Criar ListRepository (AC: Repository Spring Data JPA)
  - [ ] 5.1: Criar ListRepository interface em list/repository/ extends JpaRepository<List, UUID>
  - [ ] 5.2: Adicionar método findByOwnerId(UUID ownerId) para buscar listas do usuário
  - [ ] 5.3: Adicionar método findByInviteCode(String inviteCode) para aceitar convites
  - [ ] 5.4: Adicionar @Query custom para contar itens da lista (preparação para futura feature)

- [ ] Task 6: Criar ListTypeRepository (AC: Repository para tipos)
  - [ ] 6.1: Criar ListTypeRepository interface em list/repository/ extends JpaRepository<ListTypeEntity, Integer>
  - [ ] 6.2: Adicionar método Optional<ListTypeEntity> findBySlug(String slug)

- [ ] Task 7: Testes de Migração e Entidades (AC: Validar schema e mapeamento)
  - [ ] 7.1: Criar ListRepositoryTest com @DataJpaTest
  - [ ] 7.2: Testar criação de lista com tipo e dono válidos
  - [ ] 7.3: Testar constraint UNIQUE em invite_code
  - [ ] 7.4: Testar foreign key fk_lists_owner (cascade delete quando usuário deletado)
  - [ ] 7.5: Testar foreign key fk_lists_type (não permite type_id inválido)
  - [ ] 7.6: Testar @PreUpdate atualiza updatedAt
  - [ ] 7.7: Testar findByOwnerId retorna listas do usuário
  - [ ] 7.8: Testar findByInviteCode encontra lista por código
  - [ ] 7.9: Testar que list_types contém exatamente 4 registros após migração
  - [ ] 7.10: Testar ListType.fromSlug() converte corretamente

## Dev Notes

### 🎯 Contexto da Story

Esta é a **PRIMEIRA STORY** do Epic 2 (Gestão de Listas Pessoais), que inicia a implementação do core business do NossaLista.

**Epic 1 (COMPLETO - 5 stories done):** Estabeleceu toda infraestrutura de autenticação:
- ✅ Story 1.1: Spring Security, Flyway, database profiles (H2 dev/PostgreSQL prod)
- ✅ Story 1.2: User entity, registro com email/senha
- ✅ Story 1.3: Login JWT, JwtAuthenticationFilter
- ✅ Story 1.4: Google OAuth2, username único
- ✅ Story 1.5: Perfil e busca de usuários

**Retrospectiva Epic 1 (key learnings aplicáveis):**
- Database migrado de H2 (dev) para **PostgreSQL via Docker Compose** (commit ee6bf0c)
- Padrão estabelecido: Constructor injection sem @Autowired
- RFC 7807 para erros, SpringDoc para docs
- @PreUpdate para timestamps automáticos
- Testes BDD (Given/When/Then) com @DataJpaTest

**Objetivo Principal:** Criar **fundação de dados para listas**, estabelecendo:
1. Tabela `list_types` com 4 tipos pré-definidos (Compras, Tarefas, Wishlist, Genérica)
2. Tabela `lists` com relacionamentos para User (owner) e ListType
3. Entidades JPA mapeadas (List, ListTypeEntity)
4. Repositories com queries necessárias

**FRs Cobertos (Epics.md):**
- FR8: Usuário pode criar nova lista escolhendo tipo e nome
- FR9: Usuário pode visualizar todas listas que possui ou participa
- FR10: Usuário pode visualizar detalhes de lista específica
- FR11: Dono pode editar nome da lista
- FR12: Dono pode excluir lista
- FR13: Sistema suporta 4 tipos de lista pré-definidos
- FR14: Tipo de lista define campos disponíveis nos itens (preparação)

### 🏗️ Decisões Arquiteturais Relevantes

**Decision #002: Data Model - Campos Dinâmicos por Tipo (Architecture.md):**
> "Colunas nullable em list_items para campos dinâmicos (quantidade, due_date, url) - Simples para MVP, migrar para JSONB se necessário"

**Implicação:** Tipos de lista são **fixos e pré-definidos** (lookup table), não editáveis pelo usuário. Fields dinâmicos serão implementados em Story 3.1 (list_items).

**PostgreSQL vs Enum: Lookup Table Escolhido**

Pesquisa 2026 ([CYBERTEC PostgreSQL](https://www.cybertec-postgresql.com/en/lookup-table-or-enum-type/)) indica:
- ✅ **Lookup table:** Flexível, permite adicionar tipos futuramente sem ALTER TYPE
- ❌ **PostgreSQL ENUM:** Não permite remover valores, dificulta mudanças

**Decisão:** Usar `list_types` **lookup table** + enum Java `ListType` para type-safety no código.

**Spring Boot 4.x + Flyway (2026 Update):**

**CRITICAL:** Spring Boot 4 requer `spring-boot-starter-flyway` (não apenas `flyway-core`) ([Medium - Pranav Khodanpur](https://pranavkhodanpur.medium.com/flyway-migrations-in-spring-boot-4-x-what-changed-and-how-to-configure-it-correctly-dbe290fa4d47))

Dependências corretas:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-flyway</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

**Flyway Best Practices 2026:**
- ✅ Naming: `V<version>__<description>.sql` (V2__create_list_types_and_lists.sql)
- ✅ **NEVER modify applied migrations** (create new migration instead)
- ✅ Versões devem ser monotônicas (V1, V2, V3...)
- ✅ PostgreSQL transactions: schema changes são atomic (rollback automático se falhar)
- ✅ Location: `src/main/resources/db/migration/` (padrão Spring Boot)

**JPA Relationships Best Practices 2026:**

Pesquisa ([Baeldung Hibernate One-to-Many](https://www.baeldung.com/hibernate-one-to-many), [Thorben Janssen Best Practices](https://thorben-janssen.com/best-practices-many-one-one-many-associations-mappings/)):
- ✅ **Use @ManyToOne (unidirecional) no lado List → User** (owning side)
- ✅ **Evite @OneToMany bidirecional** (complexidade desnecessária para MVP)
- ✅ **FetchType.LAZY** como padrão (evitar N+1 queries)
- ✅ **@JoinColumn** para especificar FK column name

**Decision #007: JWT Stateless Authentication (já implementado):**
- User entity já existe em `user/domain/User.java`
- UserRepository disponível em `user/repository/UserRepository.java`
- SecurityContext já popula User autenticado

### 📦 Stack Técnico Específico

**Backend Components:**
- **Spring Boot 4.0.2** + **Java 25**
- **Spring Data JPA** (Hibernate como provedor)
- **Flyway 11.x** (via spring-boot-starter-flyway)
- **PostgreSQL** (prod) + **H2 MODE=PostgreSQL** (dev/test)

**Novas Dependências Necessárias:**
- Nenhuma! Flyway já está no pom.xml (Story 1.1)
- `spring-boot-starter-flyway` e `flyway-database-postgresql` já incluídos

**Database Profiles (já configurados):**
- `application-dev.yml`: H2 in-memory (MODE=PostgreSQL para compatibilidade)
- `application-prod.yml`: PostgreSQL via Docker Compose

**Validações:**
- Jakarta Validation (@NotBlank, @Size) na entidade List
- Constraints SQL: UNIQUE (invite_code), FK (type_id, owner_id)

### 🔐 Segurança - Considerações

**Regras de Acesso (Story 2.2+):**
- Apenas usuário autenticado pode criar listas (POST /api/lists)
- Apenas dono pode editar/excluir lista (Story 2.5, 2.6)
- invite_code permite acesso via link (Story 4.3)

**Proteção de Dados:**
- **invite_code:** VARCHAR(20) UNIQUE, gerado aleatoriamente (alfanumérico 12 chars)
- **owner_id:** FK com ON DELETE CASCADE (se usuário deletado, listas também)
- **type_id:** FK sem CASCADE (list_types são imutáveis)

**Constraints SQL:**
- `fk_lists_owner ON DELETE CASCADE`: Listas deletadas quando usuário deletado (NFR-S7)
- `fk_lists_type`: Garante type_id válido (referência a list_types)
- `invite_code UNIQUE NULLABLE`: Permite links únicos, NULL até gerado (Story 4.2)

### 🎨 Estrutura de Código Backend

**Novos Arquivos a Criar:**

```
backend/src/main/java/br/com/leoferolive/nossalista/
├── list/
│   ├── domain/
│   │   ├── List.java                         # CRIAR - Entidade JPA para lists
│   │   ├── ListType.java                      # CRIAR - Enum (SHOPPING, TASK, WISHLIST, GENERIC)
│   │   └── ListTypeEntity.java                # CRIAR - Entidade JPA para list_types (@Immutable)
│   └── repository/
│       ├── ListRepository.java                # CRIAR - Spring Data JPA repository
│       └── ListTypeRepository.java            # CRIAR - Repository para tipos

backend/src/main/resources/db/migration/
└── V2__create_list_types_and_lists.sql        # CRIAR - Migração Flyway

backend/src/test/java/br/com/leoferolive/nossalista/list/
└── repository/
    └── ListRepositoryTest.java                # CRIAR - Testes @DataJpaTest
```

**Arquivos Existentes (não modificar):**
- `user/domain/User.java`: Já existe (Story 1.2)
- `user/repository/UserRepository.java`: Já existe (Story 1.2)

**Convenções de Nomenclatura:**
- **Package list:** `br.com.leoferolive.nossalista.list`
- **Entidades:** `List`, `ListType`, `ListTypeEntity`
- **Repositories:** `<Entity>Repository`
- **Migrations:** `V<version>__<snake_case_description>.sql`

### 📋 Especificação Detalhada do Schema

**1. Tabela list_types (Lookup Table)**

```sql
CREATE TABLE list_types (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    slug VARCHAR(50) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Dados iniciais (4 tipos fixos)
INSERT INTO list_types (id, name, slug) VALUES
    (1, 'Compras', 'compras'),
    (2, 'Tarefas', 'tarefas'),
    (3, 'Wishlist', 'wishlist'),
    (4, 'Genérica', 'generica');
```

**Campos:**
- `id`: SERIAL (auto-increment) - PK
- `name`: VARCHAR(50) - Nome exibido na UI
- `slug`: VARCHAR(50) UNIQUE - Identificador lowercase (compras, tarefas, wishlist, generica)
- `created_at`: TIMESTAMP - Quando tipo foi criado

**Características:**
- **Imutável:** Tipos não são editados/removidos após criação
- **Seeded:** 4 registros inseridos na migração
- **Lookup:** Referenciada por lists.type_id

**2. Tabela lists (Entidade Principal)**

```sql
CREATE TABLE lists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    type_id INTEGER NOT NULL,
    owner_id UUID NOT NULL,
    invite_code VARCHAR(20) UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_lists_type FOREIGN KEY (type_id) REFERENCES list_types(id),
    CONSTRAINT fk_lists_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Índices para performance
CREATE INDEX idx_lists_owner_id ON lists(owner_id);
CREATE INDEX idx_lists_invite_code ON lists(invite_code);
```

**Campos:**
- `id`: UUID PRIMARY KEY - Identificador único (gerado via gen_random_uuid())
- `name`: VARCHAR(100) NOT NULL - Nome da lista ("Mercado Semanal")
- `type_id`: INTEGER NOT NULL - FK para list_types (1=Compras, 2=Tarefas...)
- `owner_id`: UUID NOT NULL - FK para users (dono da lista)
- `invite_code`: VARCHAR(20) UNIQUE NULLABLE - Código para convites (gerado em Story 4.2)
- `created_at`: TIMESTAMP NOT NULL - Data de criação
- `updated_at`: TIMESTAMP NOT NULL - Última atualização (@PreUpdate)

**Constraints:**
- `fk_lists_type`: Garante type_id válido (referência list_types.id)
- `fk_lists_owner ON DELETE CASCADE`: Deleta listas quando usuário deletado (NFR-S7)
- `invite_code UNIQUE`: Garante links de convite únicos (NULL até gerado)

**Índices:**
- `idx_lists_owner_id`: Performance para "buscar listas do usuário"
- `idx_lists_invite_code`: Performance para "aceitar convite via link"

**3. Enum Java ListType**

```java
package br.com.leoferolive.nossalista.list.domain;

/**
 * Enum representando os tipos de lista disponíveis no NossaLista.
 * Sincronizado com a tabela list_types no database.
 *
 * Tipos pré-definidos:
 * - SHOPPING: Listas de compras (com campo quantidade)
 * - TASK: Listas de tarefas (com campo due_date)
 * - WISHLIST: Listas de desejos (com campo url)
 * - GENERIC: Listas genéricas (sem campos extras)
 */
public enum ListType {
    SHOPPING("compras"),
    TASK("tarefas"),
    WISHLIST("wishlist"),
    GENERIC("generica");

    private final String slug;

    ListType(String slug) {
        this.slug = slug;
    }

    public String getSlug() {
        return slug;
    }

    public static ListType fromSlug(String slug) {
        for (ListType type : values()) {
            if (type.slug.equals(slug)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown list type slug: " + slug);
    }
}
```

**4. Entidade List (JPA)**

```java
package br.com.leoferolive.nossalista.list.domain;

import br.com.leoferolive.nossalista.user.domain.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade JPA representando uma lista compartilhada.
 *
 * Relacionamentos:
 * - ManyToOne com User (owner): Dono da lista
 * - ManyToOne com ListTypeEntity: Tipo da lista (Compras, Tarefas, etc)
 *
 * Campos dinâmicos por tipo serão implementados em list_items (Story 3.1).
 */
@Entity(name = "lists")
@Table(name = "lists")
public class List {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @NotBlank(message = "Nome da lista é obrigatório")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "type_id", nullable = false)
    private Integer typeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", referencedColumnName = "id", insertable = false, updatable = false)
    private ListTypeEntity typeEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "invite_code", unique = true, length = 20)
    private String inviteCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Retorna o tipo da lista como enum ListType.
     */
    public ListType getType() {
        if (typeEntity != null) {
            return ListType.fromSlug(typeEntity.getSlug());
        }
        // Fallback se typeEntity não carregado
        switch (typeId) {
            case 1: return ListType.SHOPPING;
            case 2: return ListType.TASK;
            case 3: return ListType.WISHLIST;
            case 4: return ListType.GENERIC;
            default: throw new IllegalStateException("Invalid typeId: " + typeId);
        }
    }

    // Getters and Setters
}
```

**Relacionamentos JPA:**
- `@ManyToOne User owner`: Unidirecional (List sabe quem é owner, User não tem coleção de listas)
- `@ManyToOne ListTypeEntity typeEntity`: Unidirecional (carregado LAZY para performance)
- `FetchType.LAZY`: Evita N+1 queries (best practice 2026)

**5. Entidade ListTypeEntity (JPA)**

```java
package br.com.leoferolive.nossalista.list.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/**
 * Entidade JPA representando tipos de lista pré-definidos.
 *
 * Tipos disponíveis:
 * 1. Compras (slug: compras)
 * 2. Tarefas (slug: tarefas)
 * 3. Wishlist (slug: wishlist)
 * 4. Genérica (slug: generica)
 *
 * Esta entidade é IMUTÁVEL - tipos não são editados após criação.
 */
@Entity(name = "list_types")
@Table(name = "list_types")
@Immutable
public class ListTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String slug;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Getters apenas (sem setters, @Immutable)
}
```

**@Immutable:** Hibernate trata como read-only, não gera UPDATEs.

### 🧪 Testes e Validação

**Testes de Migração:**

```java
@SpringBootTest
@ActiveProfiles("test")
class FlywayMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldHaveFourListTypes() {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM list_types", Integer.class);
        assertEquals(4, count);
    }

    @Test
    void shouldHaveCorrectListTypeSlugs() {
        List<String> slugs = jdbcTemplate.queryForList(
            "SELECT slug FROM list_types ORDER BY id", String.class);
        assertEquals(List.of("compras", "tarefas", "wishlist", "generica"), slugs);
    }
}
```

**Testes de Repository:**

```java
@DataJpaTest
@ActiveProfiles("test")
class ListRepositoryTest {

    @Autowired
    private ListRepository listRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldCreateListWithValidTypeAndOwner() {
        // Given
        User owner = createTestUser();
        userRepository.save(owner);

        List list = new List();
        list.setName("Mercado Semanal");
        list.setTypeId(1); // Compras
        list.setOwner(owner);

        // When
        List saved = listRepository.save(list);

        // Then
        assertNotNull(saved.getId());
        assertEquals("Mercado Semanal", saved.getName());
        assertEquals(1, saved.getTypeId());
        assertEquals(owner.getId(), saved.getOwner().getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void shouldCascadeDeleteListsWhenOwnerDeleted() {
        // Given
        User owner = createTestUser();
        userRepository.save(owner);

        List list = new List();
        list.setName("Test List");
        list.setTypeId(1);
        list.setOwner(owner);
        listRepository.save(list);

        // When
        userRepository.delete(owner);

        // Then
        assertEquals(0, listRepository.count());
    }

    @Test
    void shouldEnforceUniqueInviteCode() {
        // Given
        User owner = createTestUser();
        userRepository.save(owner);

        List list1 = new List();
        list1.setName("List 1");
        list1.setTypeId(1);
        list1.setOwner(owner);
        list1.setInviteCode("ABC123");
        listRepository.save(list1);

        List list2 = new List();
        list2.setName("List 2");
        list2.setTypeId(2);
        list2.setOwner(owner);
        list2.setInviteCode("ABC123"); // Duplicado

        // When/Then
        assertThrows(DataIntegrityViolationException.class, () -> {
            listRepository.save(list2);
        });
    }

    @Test
    void shouldFindListsByOwnerId() {
        // Given
        User owner = createTestUser();
        userRepository.save(owner);

        List list1 = new List();
        list1.setName("List 1");
        list1.setTypeId(1);
        list1.setOwner(owner);
        listRepository.save(list1);

        List list2 = new List();
        list2.setName("List 2");
        list2.setTypeId(2);
        list2.setOwner(owner);
        listRepository.save(list2);

        // When
        List<List> lists = listRepository.findByOwnerId(owner.getId());

        // Then
        assertEquals(2, lists.size());
    }
}
```

### 🚨 Armadilhas Comuns a Evitar

1. **Modificar migração aplicada** - NUNCA alterar V2 após aplicada, criar V3 para mudanças
2. **Esquecer spring-boot-starter-flyway** - Spring Boot 4 requer starter (não apenas flyway-core)
3. **@OneToMany bidirecional desnecessário** - User NÃO precisa ter List<Lista> lists (complexidade)
4. **FetchType.EAGER** - Usar LAZY para evitar N+1 queries (best practice 2026)
5. **invite_code NOT NULL** - Deve ser NULLABLE, gerado apenas quando usuário cria link (Story 4.2)
6. **Editar list_types** - Tipos são fixos (seeded), não criar UI para edição
7. **Esquecer ON DELETE CASCADE** - owner_id deve ter CASCADE para deletar listas quando usuário removido
8. **Duplicar typeId e typeEntity** - usar `insertable = false, updatable = false` em typeEntity
9. **Esquecer @PreUpdate** - updatedAt deve atualizar automaticamente
10. **SERIAL vs UUID** - list_types usa SERIAL (lookup), lists usa UUID (entidade principal)

### 🔗 Relacionamento com Outras Stories

**Depende de:**
- ✅ Story 1.1: Flyway configurado, database profiles (H2 dev, PostgreSQL prod)
- ✅ Story 1.2: User entity, UserRepository (referenciado por lists.owner_id)
- ✅ Epic 1 COMPLETO: Autenticação funcionando, JwtAuthenticationFilter

**Próximas Stories Usarão:**
- Story 2.2: Criar nova lista (POST /api/lists) usará List entity e ListRepository
- Story 2.3: Listar listas (GET /api/lists) usará findByOwnerId()
- Story 2.4: Ver detalhes (GET /api/lists/{id}) usará List entity
- Story 2.5: Editar nome (PATCH /api/lists/{id}) usará updatedAt (@PreUpdate)
- Story 2.6: Excluir lista (DELETE /api/lists/{id}) usará cascade delete
- Story 3.1: Itens de lista usarão type_id para validar campos dinâmicos
- Story 4.2: Gerar link de convite usará invite_code (VARCHAR 20 chars)
- Story 4.3: Aceitar convite usará findByInviteCode()

**Esta Story Habilita:**
- ✅ Fundação de dados para gestão de listas
- ✅ Tipos de lista pré-definidos e imutáveis
- ✅ Relacionamento Owner → Lists (cascade delete)
- ✅ Base para convites via invite_code (implementação em Story 4.2)

### 📊 Critérios de Aceitação - Checklist

Antes de marcar esta story como completa, verificar:

✅ Arquivo V2__create_list_types_and_lists.sql criado em db/migration/
✅ Tabela list_types criada com 4 tipos pré-inseridos
✅ Tabela lists criada com todos os campos especificados
✅ Foreign key fk_lists_type criada (type_id → list_types.id)
✅ Foreign key fk_lists_owner criada (owner_id → users.id ON DELETE CASCADE)
✅ Índices idx_lists_owner_id e idx_lists_invite_code criados
✅ Constraint UNIQUE em invite_code
✅ Enum ListType criado com 4 valores + método fromSlug()
✅ Entidade ListTypeEntity criada com @Immutable
✅ Entidade List criada com validações (@NotBlank, @Size)
✅ @ManyToOne relacionamentos configurados (owner, typeEntity)
✅ @PreUpdate implementado para updatedAt
✅ ListRepository criado com findByOwnerId() e findByInviteCode()
✅ ListTypeRepository criado com findBySlug()
✅ Migração executa sem erros em H2 (test) e PostgreSQL (dev)
✅ Teste verifica 4 registros em list_types após migração
✅ Teste cria lista com tipo e owner válidos
✅ Teste valida constraint UNIQUE em invite_code
✅ Teste valida CASCADE DELETE (owner deletado → listas deletadas)
✅ Teste valida FK type_id (não permite type_id inválido)
✅ Teste valida @PreUpdate atualiza updatedAt
✅ Teste findByOwnerId retorna listas do usuário
✅ Teste findByInviteCode encontra lista por código

### Project Structure Notes

**Alinhamento com Estrutura de Projeto Unificada:**

Esta story cria o novo módulo `list/` seguindo os padrões estabelecidos em Epic 1 (módulo `user/`).

**Novos Módulos/Pacotes Criados:**

```
backend/src/main/java/br/com/leoferolive/nossalista/
└── list/                                      # NOVO MÓDULO
    ├── domain/
    │   ├── List.java                         # Entidade JPA principal
    │   ├── ListType.java                      # Enum para tipos
    │   └── ListTypeEntity.java                # Entidade lookup table
    └── repository/
        ├── ListRepository.java                # Spring Data JPA
        └── ListTypeRepository.java            # Repository lookup

backend/src/main/resources/db/migration/
└── V2__create_list_types_and_lists.sql       # Segunda migração Flyway
```

**Padrões de Código Backend Estabelecidos (Epic 1):**

- **Package por domínio:** list/, user/, auth/ (DDD-lite)
- **Constructor injection:** Sem @Autowired
- **@PreUpdate/@PrePersist:** Timestamps automáticos
- **FetchType.LAZY:** Evitar N+1 queries
- **Validações:** Jakarta Validation (@NotBlank, @Size)
- **Testes:** @DataJpaTest para repositories
- **JavaDoc:** Em português

**Decisões de Nomenclatura:**

- **Entidades:** PascalCase (List, ListTypeEntity, não "Lista")
- **Enums:** SCREAMING_SNAKE_CASE (SHOPPING, TASK, WISHLIST, GENERIC)
- **Tables:** snake_case (list_types, lists)
- **Migrations:** V<version>__snake_case_description.sql

### References

Todos os detalhes técnicos com fontes de documentação:

**Epics e Stories:**
- [Fonte: _bmad-output/planning-artifacts/epics.md#Story-2.1]
  - Epic 2: Gestão de Listas Pessoais (linhas 581-641)
  - Story 2.1: Modelagem de Dados de Listas e Tipos (linhas 586-609)
  - FR8-14: Requisitos funcionais de listas

**PRD:**
- [Fonte: _bmad-output/planning-artifacts/prd.md]
  - FR8: Criar lista escolhendo tipo e nome
  - FR9: Visualizar todas listas do usuário
  - FR13: 4 tipos de lista pré-definidos (Compras, Tarefas, Wishlist, Genérica)
  - FR14: Tipo define campos disponíveis em itens

**Decisões Arquiteturais:**
- [Fonte: _bmad-output/planning-artifacts/architecture.md]
  - **Decision #002:** Data Model - Campos nullable para dinâmica (linhas 611-650)
  - **Flyway Migrations:** V{version}__{description}.sql (linhas 1956-1964)
  - **Repository Structure:** Monorepo (Decision #001)

**Story Anterior Relevante:**
- [Fonte: _bmad-output/implementation-artifacts/1-5-perfil-e-busca-de-usuarios.md]
  - User entity em user/domain/User.java
  - UserRepository em user/repository/UserRepository.java
  - Padrões de código: Constructor injection, @PreUpdate, testes BDD

**Commits Recentes (Git Intelligence):**
- Commit ee6bf0c: "refactor(database): migrate dev environment from H2 to PostgreSQL via Docker Compose"
  - Database agora é PostgreSQL mesmo em dev (não H2)
  - Docker Compose para desenvolvimento local
  - H2 apenas para testes (@DataJpaTest)

**Documentação Técnica Externa (2026):**

**Spring Boot 4.x + Flyway:**
- [Flyway Migrations in Spring Boot 4.x | Medium](https://pranavkhodanpur.medium.com/flyway-migrations-in-spring-boot-4-x-what-changed-and-how-to-configure-it-correctly-dbe290fa4d47)
  - CRITICAL: Usar spring-boot-starter-flyway (não apenas flyway-core)
  - Flyway não auto-configura em Spring Boot 4 sem starter
- [Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)

**PostgreSQL Enum vs Lookup Table:**
- [Lookup Table or Enum Type? | CYBERTEC PostgreSQL](https://www.cybertec-postgresql.com/en/lookup-table-or-enum-type/)
  - Lookup table é mais flexível (adicionar/remover valores)
  - Enum PostgreSQL não permite remover valores
  - Decisão: Lookup table (list_types) para MVP

**JPA Relationships Best Practices:**
- [Hibernate One-to-Many | Baeldung](https://www.baeldung.com/hibernate-one-to-many)
  - Use @ManyToOne (unidirecional) como owning side
  - Evite @OneToMany bidirecional (complexidade)
  - FetchType.LAZY para evitar N+1 queries
- [Best Practices Many-to-One | Thorben Janssen](https://thorben-janssen.com/best-practices-many-one-one-many-associations-mappings/)

**Flyway Best Practices:**
- [Database Migrations with Flyway | Baeldung](https://www.baeldung.com/database-migrations-with-flyway)
  - NEVER modify applied migrations (create new one)
  - Versões monotônicas (V1, V2, V3...)
  - PostgreSQL: Schema changes são atomic (transaction safe)

## Dev Agent Record

### Agent Model Used

Story criada por: Claude Sonnet 4.5 (claude-sonnet-4-5-20250929)

### Debug Log References

N/A - Story em fase ready-for-dev (ainda não implementada)

### Completion Notes List

**Story Creation Notes:**

Esta story foi criada com análise exaustiva de:
- ✅ Epic 2 completo (7 stories analisadas)
- ✅ PRD (45 FRs, 19 NFRs mapeados)
- ✅ Architecture.md (Decisões arquiteturais #001, #002, #007)
- ✅ Story 1.5 (última do Epic 1) para aprender padrões estabelecidos
- ✅ Commits recentes (último commit: ee6bf0c - PostgreSQL via Docker Compose)
- ✅ Web research 2026 (Spring Boot 4 Flyway, PostgreSQL enum vs lookup table, JPA relationships)

**Contexto Incorporado:**
- Database mudou para PostgreSQL em dev (não mais H2 in-memory)
- Spring Boot 4 requer spring-boot-starter-flyway (crítico!)
- Lookup table escolhido em vez de PostgreSQL enum (flexibilidade)
- @ManyToOne unidirecional (best practice 2026)
- FetchType.LAZY para performance
- Padrões de código do Epic 1 preservados

**Dev-Ready Guardrails:**
- Schema SQL completo especificado
- Entidades JPA com código exemplo
- Testes detalhados com exemplos
- Armadilhas comuns documentadas
- Referências com links externos atualizados (2026)

---

**Code Review Fixes (2026-02-12):**

Reviewed by: Claude Sonnet 4.5 (adversarial code review)

**Issues Found:** 8 total (3 HIGH, 3 MEDIUM, 2 LOW)
**Issues Fixed:** 6 (3 HIGH, 3 MEDIUM)

**HIGH Issues Fixed:**
1. ✅ **Migration SQL PostgreSQL compatibility** - Mudado `random_uuid()` (H2) → `gen_random_uuid()` (PostgreSQL)
   - File: V2__create_list_types_and_lists.sql:21
   - Motivo: Dev environment agora é PostgreSQL (commit ee6bf0c), H2 syntax quebra em prod

2. ✅ **ListTypeEntity imutabilidade violada** - Removidos todos os setters
   - File: ListTypeEntity.java:44-69
   - Motivo: @Immutable significa read-only, setters permitiam modificações indevidas

3. ✅ **Testes setavam timestamps manualmente** - Removidos setCreatedAt/setUpdatedAt
   - File: ListRepositoryTest.java (múltiplos testes)
   - Motivo: @PrePersist/@PreUpdate devem fazer o trabalho, testes estavam escondendo bugs

**MEDIUM Issues Fixed:**
4. ✅ **pom.xml não documentado** - Adicionado à File List
   - Mudança: flyway-core → spring-boot-starter-flyway (Spring Boot 4 requirement)

5. ✅ **@SpringBootTest desnecessário** - Mudado para @DataJpaTest
   - File: ListRepositoryTest.java:26
   - Motivo: @DataJpaTest é 3-5x mais rápido, isolado, conforme spec da story

6. ✅ **@Sql manual redundante** - Removido, Flyway auto-executa
   - File: ListRepositoryTest.java:27
   - Motivo: Flyway gerencia migrations automaticamente

**LOW Issues (não fixados, apenas observados):**
7. ⚠️ **Namespace collision** (java.util.List vs List entity) - Aceitável, comum em Java
8. ⚠️ **Falta helper generateInviteCode()** - Será implementado na Story 2.2

**Resultado:**
- ✅ Todos os Acceptance Criteria agora 100% implementados
- ✅ Migration compatível com PostgreSQL production
- ✅ Entidades JPA seguem best practices (imutabilidade, timestamps automáticos)
- ✅ Testes validam comportamento real (@PrePersist/@PreUpdate)
- ✅ File List completo e preciso

**Próximos Passos:**
1. ✅ Code review completo - APROVADO com fixes aplicados
2. Marcar story como "done" após validação
3. Criar Story 2.2 (Criar Nova Lista - POST /api/lists)

### File List

**Arquivos Criados:**

```
backend/src/main/java/br/com/leoferolive/nossalista/list/domain/List.java
backend/src/main/java/br/com/leoferolive/nossalista/list/domain/ListType.java
backend/src/main/java/br/com/leoferolive/nossalista/list/domain/ListTypeEntity.java
backend/src/main/java/br/com/leoferolive/nossalista/list/repository/ListRepository.java
backend/src/main/java/br/com/leoferolive/nossalista/list/repository/ListTypeRepository.java
backend/src/main/resources/db/migration/V2__create_list_types_and_lists.sql
backend/src/test/java/br/com/leoferolive/nossalista/list/repository/ListRepositoryTest.java
```

**Arquivos Modificados:**

```
backend/pom.xml - Migração de dependência Flyway: flyway-core → spring-boot-starter-flyway (Spring Boot 4 requirement)
```

**Arquivos Referenciados (não modificados):**

```
backend/src/main/java/br/com/leoferolive/nossalista/user/domain/User.java
backend/src/main/java/br/com/leoferolive/nossalista/user/repository/UserRepository.java
```

