# Story 3.1: Modelagem de Dados de Itens

Status: done

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Story

As a desenvolvedor,
I want criar a estrutura de dados para itens de lista,
So that o sistema possa armazenar itens com campos dinâmicos por tipo.

## Acceptance Criteria

**Given** migração Flyway V4__create_list_items.sql
**When** executada
**Then** tabela list_items deve ter: id (UUID), list_id (FK), name, checked (BOOLEAN, default false), quantity (nullable), due_date (nullable), url (nullable), position (INTEGER), created_by (FK), created_at, updated_at
**And** índices: idx_list_items_list_id, idx_list_items_position
**And** constraints: fk_list_items_list (CASCADE), fk_list_items_creator

**Given** entidade ListItem no backend
**When** mapeada via JPA
**Then** deve ter campos: id, list, name, checked, quantity, dueDate, url, position, createdBy, createdAt, updatedAt
**And** @PreUpdate para atualizar updated_at

**Given** lista do tipo Compras/Tarefas/Wishlist/Genérica
**When** item é criado
**Then** campos nullable correspondentes devem ser aplicados (quantity/due_date/url ou NULL)

## Tasks / Subtasks

- [x] Task 1: Criar migração Flyway V4__create_list_items.sql (AC: Tabela list_items)
  - [x] 1.1: Criar arquivo V4__create_list_items.sql em src/main/resources/db/migration/
  - [x] 1.2: Criar tabela list_items com id UUID PRIMARY KEY
  - [x] 1.3: Adicionar list_id UUID NOT NULL com FK para lists(id) ON DELETE CASCADE
  - [x] 1.4: Adicionar name VARCHAR(200) NOT NULL
  - [x] 1.5: Adicionar checked BOOLEAN DEFAULT false
  - [x] 1.6: Adicionar quantity INTEGER NULLABLE (para tipo Compras)
  - [x] 1.7: Adicionar due_date TIMESTAMP NULLABLE (para tipo Tarefas)
  - [x] 1.8: Adicionar url VARCHAR(500) NULLABLE (para tipo Wishlist)
  - [x] 1.9: Adicionar position INTEGER NOT NULL DEFAULT 0
  - [x] 1.10: Adicionar created_by UUID NOT NULL com FK para users(id)
  - [x] 1.11: Adicionar created_at e updated_at TIMESTAMP com DEFAULT NOW()
  - [x] 1.12: Criar índice idx_list_items_list_id ON list_items(list_id)
  - [x] 1.13: Criar índice idx_list_items_position ON list_items(list_id, position)
  - [x] 1.14: Testar migração localmente (154 testes passando)

- [x] Task 2: Criar entidade ListItem (AC: Entidade JPA para list_items)
  - [x] 2.1: Criar ListItem entity em listitem/domain/ com @Entity(name="list_items")
  - [x] 2.2: Adicionar campos: id (UUID), name (String), checked (boolean), quantity (Integer), dueDate (LocalDateTime), url (String), position (Integer)
  - [x] 2.3: Adicionar @ManyToOne com List (list) com FetchType.LAZY
  - [x] 2.4: Adicionar @ManyToOne com User (createdBy) com FetchType.LAZY
  - [x] 2.5: Adicionar @PreUpdate para atualizar updatedAt automaticamente
  - [x] 2.6: Adicionar validações: @NotBlank para name, @Size(max=200) para name, @Size(max=500) para url
  - [x] 2.7: Adicionar construtor padrão e métodos getters/setters

- [x] Task 3: Criar ListItemRepository (AC: Repository Spring Data JPA)
  - [x] 3.1: Criar ListItemRepository interface em listitem/repository/ extends JpaRepository<ListItem, UUID>
  - [x] 3.2: Adicionar método List<ListItem> findByListIdOrderByPositionAsc(UUID listId)
  - [x] 3.3: Adicionar método Optional<ListItem> findByIdAndListId(UUID id, UUID listId)
  - [x] 3.4: Adicionar método @Query para buscar com JOIN FETCH (findByIdWithDetails)
  - [x] 3.5: Adicionar método Long countByListId(UUID listId)

- [x] Task 4: Criar DTOs para ListItem (AC: Data Transfer Objects)
  - [x] 4.1: Criar ListItemResponseDTO com todos os campos + createdByUser (username, avatar)
  - [x] 4.2: Criar CreateItemRequestDTO com name, quantity, dueDate, url (validações)
  - [x] 4.3: Criar UpdateItemRequestDTO com campos opcionais para atualização
  - [x] 4.4: Criar ListItemMapper para conversão Entity <-> DTO

- [x] Task 5: Testes de Migração e Entidades (AC: Validar schema e mapeamento)
  - [x] 5.1: Criar ListItemRepositoryTest com @SpringBootTest
  - [x] 5.2: Testar criação de item com lista e criador válidos
  - [x] 5.3: Testar constraint CASCADE configurada na migração
  - [x] 5.4: Testar findByListIdOrderByPositionAsc retorna itens ordenados
  - [x] 5.5: Testar campos nullable (quantity, due_date, url)
  - [x] 5.6: Testar @PreUpdate atualiza updatedAt
  - [x] 5.7: Testar validação @NotBlank em name
  - [x] 5.8: Testar demais métodos do repository (findByIdAndListId, countByListId, findByIdWithDetails)

## Dev Notes

### 🎯 Contexto da Story

Esta é a **PRIMEIRA STORY** do Epic 3 (Gestão de Itens), que implementa a fundação de dados para gerenciamento de itens dentro das listas.

**Epic 2 (COMPLETO - 6 stories done):** Estabeleceu toda infraestrutura de listas:
- ✅ Story 2.1: Modelagem de dados (List, ListType entities, migrations V2)
- ✅ Story 2.2: Criar nova lista (POST /api/lists)
- ✅ Story 2.3: Listar todas as listas (GET /api/lists)
- ✅ Story 2.4: Ver detalhes de uma lista (GET /api/lists/{id})
- ✅ Story 2.5: Editar nome da lista (PATCH /api/lists/{id})
- ✅ Story 2.6: Excluir lista (DELETE /api/lists/{id})

**Retrospectiva Epic 2 (key learnings aplicáveis):**
- Padrão estabelecido: `findByIdWithDetails()` para evitar LazyInitializationException
- `@Transactional` obrigatório para operações de escrita
- RFC 7807 para erros, SpringDoc para docs
- @PreUpdate/@PrePersist para timestamps automáticos
- Testes BDD (Given/When/Then) com @DataJpaTest
- PostgreSQL via Docker Compose (H2 apenas para testes)
- CASCADE via database constraints para integridade referencial

**Objetivo Principal:** Criar **fundação de dados para itens de lista**, estabelecendo:
1. Tabela `list_items` com campos dinâmicos por tipo (quantity, due_date, url)
2. Entidade JPA ListItem mapeada com relacionamentos
3. Repository com queries necessárias
4. Base para operações CRUD de itens (Stories 3.2-3.6)

**FRs Cobertos (Epics.md):**
- FR14: Tipo de lista define campos disponíveis nos itens
- FR15: Participante pode adicionar itens (preparação)
- FR16: Participante pode editar itens (preparação)
- FR17: Participante pode remover itens (preparação)
- FR18: Participante pode marcar/desmarcar item (preparação)
- FR19: Campo quantidade para tipo Compras
- FR20: Campo due_date para tipo Tarefas
- FR21: Campo URL para tipo Wishlist
- FR22: Sistema registra quem criou cada item

### 🏗️ Decisões Arquiteturais Relevantes

**Decision #002: Data Model - Campos Dinâmicos por Tipo (Architecture.md):**
> "Colunas nullable em list_items para campos dinâmicos (quantidade, due_date, url) - Simples para MVP, migrar para JSONB se necessário"

**Estratégia de Campos Dinâmicos:**

| Tipo de Lista | Campos Específicos | Coluna SQL |
|--------------|-------------------|------------|
| Compras (SHOPPING) | Quantidade | quantity (INTEGER) |
| Tarefas (TASK) | Prazo/Data | due_date (TIMESTAMP) |
| Wishlist | URL do produto | url (VARCHAR 500) |
| Genérica | Nenhum | Apenas name + checked |

**Vantagens da Abordagem (Colunas Nullable):**
- ✅ Simples de implementar e entender
- ✅ Type-safe no banco (INTEGER para quantidade, TIMESTAMP para data)
- ✅ Indexável individualmente
- ✅ Fácil de validar (constraint CHECK opcional)

**Desvantagens:**
- ⚠️ Schema fixo (adicionar novo campo requer ALTER TABLE)
- ⚠️ NULLs em todas as linhas para campos não aplicáveis

**Alternativa (JSONB):** Seria mais flexível mas complexa para queries e validações. Decisão: nullable columns para MVP.

**Cascade Deletion Pattern (Story 2.6):**

```sql
-- V3__create_list_items.sql
ALTER TABLE list_items
ADD CONSTRAINT fk_list_items_list
FOREIGN KEY (list_id) REFERENCES lists(id) ON DELETE CASCADE;
```

Quando uma lista é deletada (Story 2.6), todos os seus itens são automaticamente removidos via CASCADE.

**Position Field (Ordenação):**

O campo `position` (INTEGER) permite ordenação customizada dos itens:
- Valor padrão: 0
- Ordenação: ASC (primeiro item = position 0)
- Futuro: Drag-and-drop para reordenar (fora do MVP)

### 📦 Stack Técnico Específico

**Backend Components:**
- **Spring Boot 4.0.2** + **Java 25**
- **Spring Data JPA** (Hibernate como provedor)
- **Flyway 11.x** (via spring-boot-starter-flyway)
- **PostgreSQL** (prod) + **H2 MODE=PostgreSQL** (test)

**Novas Dependências:** Nenhuma (Flyway e JPA já configurados)

**Database Profiles (já configurados):**
- `application-dev.yml`: PostgreSQL via Docker Compose
- `application-prod.yml`: PostgreSQL via K3s
- `application-test.yml`: H2 in-memory

### 🔐 Segurança - Considerações

**Regras de Acesso (Stories 3.2+):**
- Qualquer participante da lista pode criar/editar/remover itens (FR29)
- Dono e membros têm permissões iguais para itens
- created_by registra quem criou (auditoria)

**Proteção de Dados:**
- **list_id FK CASCADE:** Itens deletados quando lista é deletada
- **created_by FK:** Referência ao usuário criador (não cascade)
- **Campos validados:** name obrigatório, url máximo 500 chars

**Constraints SQL:**
- `fk_list_items_list ON DELETE CASCADE`: Itens deletados com lista
- `fk_list_items_creator`: Referência ao usuário (sem cascade)

### 🎨 Estrutura de Código Backend

**Novos Arquivos a Criar:**

```
backend/src/main/java/br/com/leoferolive/nossalista/
├── listitem/
│   ├── domain/
│   │   └── ListItem.java               # CRIAR - Entidade JPA para list_items
│   ├── repository/
│   │   └── ListItemRepository.java     # CRIAR - Spring Data JPA repository
│   └── dto/
│       ├── ListItemResponseDTO.java    # CRIAR - DTO de resposta
│       ├── CreateItemRequestDTO.java   # CRIAR - DTO de criação
│       ├── UpdateItemRequestDTO.java   # CRIAR - DTO de atualização
│       └── ListItemMapper.java         # CRIAR - Mapper Entity <-> DTO

backend/src/main/resources/db/migration/
└── V3__create_list_items.sql            # CRIAR - Migração Flyway

backend/src/test/java/br/com/leoferolive/nossalista/listitem/
└── repository/
    └── ListItemRepositoryTest.java      # CRIAR - Testes @DataJpaTest
```

**Arquivos Existentes (não modificar):**
- `list/domain/List.java`: Já existe (Story 2.1)
- `list/repository/ListRepository.java`: Já existe (Story 2.1)
- `user/domain/User.java`: Já existe (Story 1.2)

**Convenções de Nomenclatura:**
- **Package listitem:** `br.com.leoferolive.nossalista.listitem` (não "item" para evitar conflito com java.lang item)
- **Entidade:** `ListItem` (não "Item" para clareza)
- **Table:** `list_items` (snake_case, plural)
- **Migrations:** `V<version>__<snake_case_description>.sql`

### 📋 Especificação Detalhada do Schema

**1. Tabela list_items**

```sql
CREATE TABLE list_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    list_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    checked BOOLEAN NOT NULL DEFAULT false,
    quantity INTEGER,
    due_date TIMESTAMP,
    url VARCHAR(500),
    position INTEGER NOT NULL DEFAULT 0,
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_list_items_list
        FOREIGN KEY (list_id) REFERENCES lists(id) ON DELETE CASCADE,
    CONSTRAINT fk_list_items_creator
        FOREIGN KEY (created_by) REFERENCES users(id)
);

-- Índices para performance
CREATE INDEX idx_list_items_list_id ON list_items(list_id);
CREATE INDEX idx_list_items_position ON list_items(list_id, position);
```

**Campos:**
- `id`: UUID PRIMARY KEY - Identificador único
- `list_id`: UUID NOT NULL - FK para lists (CASCADE DELETE)
- `name`: VARCHAR(200) NOT NULL - Nome do item
- `checked`: BOOLEAN DEFAULT false - Status de concluído
- `quantity`: INTEGER NULLABLE - Quantidade (para tipo Compras)
- `due_date`: TIMESTAMP NULLABLE - Prazo (para tipo Tarefas)
- `url`: VARCHAR(500) NULLABLE - URL do produto (para tipo Wishlist)
- `position`: INTEGER DEFAULT 0 - Ordenação do item na lista
- `created_by`: UUID NOT NULL - FK para users (quem criou)
- `created_at`: TIMESTAMP - Data de criação
- `updated_at`: TIMESTAMP - Última atualização (@PreUpdate)

**Constraints:**
- `fk_list_items_list ON DELETE CASCADE`: Deleta itens quando lista é deletada
- `fk_list_items_creator`: Referência ao usuário criador

**Índices:**
- `idx_list_items_list_id`: Performance para "buscar itens da lista"
- `idx_list_items_position`: Performance para ordenação por position

**2. Entidade ListItem (JPA)**

```java
package br.com.leoferolive.nossalista.listitem.domain;

import br.com.leoferolive.nossalista.list.domain.List;
import br.com.leoferolive.nossalista.user.domain.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade JPA representando um item de lista.
 *
 * Campos dinâmicos por tipo:
 * - quantity: Usado em listas do tipo Compras
 * - dueDate: Usado em listas do tipo Tarefas
 * - url: Usado em listas do tipo Wishlist
 *
 * Relacionamentos:
 * - ManyToOne com List: Lista à qual o item pertence
 * - ManyToOne com User: Usuário que criou o item
 */
@Entity(name = "list_items")
@Table(name = "list_items")
public class ListItem {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @NotBlank(message = "Nome do item é obrigatório")
    @Size(max = 200, message = "Nome deve ter no máximo 200 caracteres")
    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false)
    private boolean checked = false;

    @Column
    private Integer quantity;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Size(max = 500, message = "URL deve ter no máximo 500 caracteres")
    @Column(length = 500)
    private String url;

    @Column(nullable = false)
    private Integer position = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id", nullable = false)
    private List list;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

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

    // Getters and Setters
}
```

**3. Repository ListItemRepository**

```java
package br.com.leoferolive.nossalista.listitem.repository;

import br.com.leoferolive.nossalista.listitem.domain.ListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ListItemRepository extends JpaRepository<ListItem, UUID> {

    /**
     * Busca todos os itens de uma lista ordenados por position ASC.
     */
    List<ListItem> findByListIdOrderByPositionAsc(UUID listId);

    /**
     * Busca item específico dentro de uma lista.
     */
    Optional<ListItem> findByIdAndListId(UUID id, UUID listId);

    /**
     * Busca item com todos os relacionamentos carregados (evita LazyInitializationException).
     */
    @Query("SELECT li FROM list_items li " +
           "LEFT JOIN FETCH li.list " +
           "LEFT JOIN FETCH li.createdBy " +
           "WHERE li.id = :id")
    Optional<ListItem> findByIdWithDetails(@Param("id") UUID id);

    /**
     * Conta quantos itens existem em uma lista.
     */
    Long countByListId(UUID listId);
}
```

### 🧪 Testes e Validação

**Testes de Repository:**

```java
@DataJpaTest
@ActiveProfiles("test")
class ListItemRepositoryTest {

    @Autowired
    private ListItemRepository listItemRepository;

    @Autowired
    private ListRepository listRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldCreateItemWithValidListAndCreator() {
        // Given
        User creator = createTestUser();
        userRepository.save(creator);

        List list = createTestList(creator);
        listRepository.save(list);

        ListItem item = new ListItem();
        item.setName("Arroz");
        item.setList(list);
        item.setCreatedBy(creator);
        item.setQuantity(2); // Campo específico de Compras

        // When
        ListItem saved = listItemRepository.save(item);

        // Then
        assertNotNull(saved.getId());
        assertEquals("Arroz", saved.getName());
        assertEquals(2, saved.getQuantity());
        assertFalse(saved.isChecked());
        assertEquals(0, saved.getPosition());
    }

    @Test
    void shouldCascadeDeleteItemsWhenListDeleted() {
        // Given
        User owner = createTestUser();
        userRepository.save(owner);

        List list = createTestList(owner);
        listRepository.save(list);

        ListItem item = new ListItem();
        item.setName("Item Test");
        item.setList(list);
        item.setCreatedBy(owner);
        listItemRepository.save(item);

        // When
        listRepository.delete(list);

        // Then
        assertEquals(0, listItemRepository.count());
    }

    @Test
    void shouldFindItemsOrderedByPosition() {
        // Given: lista com 3 itens em ordem diferente
        // ... setup ...

        // When
        List<ListItem> items = listItemRepository.findByListIdOrderByPositionAsc(list.getId());

        // Then
        assertEquals(3, items.size());
        assertEquals("Primeiro", items.get(0).getName());
        assertEquals("Segundo", items.get(1).getName());
        assertEquals("Terceiro", items.get(2).getName());
    }
}
```

### 🚨 Armadilhas Comuns a Evitar

1. **Esquecer ON DELETE CASCADE** - Itens devem ser deletados automaticamente quando lista é deletada
2. **Campo position não nullable** - Sempre ter valor padrão (0) para evitar NULL
3. **FetchType.EAGER** - Usar LAZY para list e createdBy (evitar N+1)
4. **Esquecer @PreUpdate** - updatedAt deve atualizar automaticamente
5. **Nome da entidade** - Usar "list_items" (não "items") para clareza
6. **Package name** - Usar "listitem" (não "item") para evitar conflito com java.lang
7. **URL length** - Definir limite VARCHAR(500) para URLs longas
8. **due_date vs dueDate** - Mapear corretamente snake_case -> camelCase

### 🔗 Relacionamento com Outras Stories

**Depende de:**
- ✅ Story 2.1: Tabelas lists e list_types (FK list_id)
- ✅ Story 1.2: Tabela users (FK created_by)
- ✅ Epic 1 e 2 COMPLETOS

**Próximas Stories Usarão:**
- Story 3.2: Adicionar item (POST /api/lists/{id}/items) usará ListItem entity
- Story 3.3: Listar itens (GET /api/lists/{id}/items) usará findByListIdOrderByPositionAsc
- Story 3.4: Marcar item (PATCH) usará campo checked
- Story 3.5: Editar item usará campos dinâmicos (quantity, due_date, url)
- Story 3.6: Remover item usará delete com CASCADE (já configurado)
- Story 5.2: WebSocket broadcast de itens usará ListItem para payload
- Story 6.2: Activity log de itens referenciará target_id (list_items.id)

**Esta Story Habilita:**
- ✅ Fundação de dados para gestão de itens
- ✅ Campos dinâmicos por tipo (quantity, due_date, url)
- ✅ Rastreamento de criador (created_by)
- ✅ Ordenação por position
- ✅ Integridade referencial com CASCADE

### 📊 Critérios de Aceitação - Checklist

Antes de marcar esta story como completa, verificar:

✅ Arquivo V3__create_list_items.sql criado em db/migration/
✅ Tabela list_items criada com todos os campos especificados
✅ Foreign key fk_list_items_list criada (list_id → lists.id ON DELETE CASCADE)
✅ Foreign key fk_list_items_creator criada (created_by → users.id)
✅ Índices idx_list_items_list_id e idx_list_items_position criados
✅ Entidade ListItem criada com @Entity(name="list_items")
✅ Campos mapeados: name, checked, quantity, dueDate, url, position
✅ @ManyToOne com List (LAZY) configurado
✅ @ManyToOne com User/createdBy (LAZY) configurado
✅ @PreUpdate implementado para updatedAt
✅ Validações @NotBlank e @Size aplicadas
✅ ListItemRepository criado com métodos necessários
✅ DTOs criados (Response, CreateRequest, UpdateRequest)
✅ Mapper criado para conversão Entity <-> DTO
✅ Migração executa sem erros em PostgreSQL
✅ Teste verifica criação de item com lista e criador válidos
✅ Teste verifica CASCADE DELETE (lista deletada → itens deletados)
✅ Teste verifica ordenação por position
✅ Teste verifica campos nullable

### Project Structure Notes

**Alinhamento com Estrutura de Projeto Unificada:**

Esta story cria o novo módulo `listitem/` seguindo os padrões estabelecidos em Epic 1 (módulo `user/`) e Epic 2 (módulo `list/`).

**Novos Módulos/Pacotes Criados:**

```
backend/src/main/java/br/com/leoferolive/nossalista/
└── listitem/                                      # NOVO MÓDULO
    ├── domain/
    │   └── ListItem.java                         # Entidade JPA principal
    ├── repository/
    │   └── ListItemRepository.java               # Spring Data JPA
    └── dto/
        ├── ListItemResponseDTO.java              # DTO de resposta
        ├── CreateItemRequestDTO.java             # DTO de criação
        ├── UpdateItemRequestDTO.java             # DTO de atualização
        └── ListItemMapper.java                   # Mapper

backend/src/main/resources/db/migration/
└── V3__create_list_items.sql                    # Terceira migração Flyway

backend/src/test/java/br/com/leoferolive/nossalista/listitem/
└── repository/
    └── ListItemRepositoryTest.java              # Testes @DataJpaTest
```

**Padrões de Código Backend Estabelecidos:**

- **Package por domínio:** listitem/, list/, user/, auth/ (DDD-lite)
- **Constructor injection:** Sem @Autowired
- **@PreUpdate/@PrePersist:** Timestamps automáticos
- **FetchType.LAZY:** Evitar N+1 queries
- **Validações:** Jakarta Validation (@NotBlank, @Size)
- **Testes:** @DataJpaTest para repositories
- **Migrations:** PostgreSQL syntax (gen_random_uuid())

### References

**Epics e Stories:**
- [Fonte: _bmad-output/planning-artifacts/epics.md#Story-3.1]
  - Epic 3: Gestão de Itens
  - Story 3.1: Modelagem de Dados de Itens
  - FR14-22: Requisitos funcionais de itens

**Story Anterior Relevante:**
- [Fonte: _bmad-output/implementation-artifacts/2-1-modelagem-de-dados-de-listas-e-tipos.md]
  - Padrão de modelagem de dados (migrations, entidades, repositories)
  - Convenções de nomenclatura
  - Testes @DataJpaTest

**Decisões Arquiteturais:**
- [Fonte: _bmad-output/planning-artifacts/architecture.md]
  - **Decision #002:** Data Model - Campos nullable para dinâmica
  - **Flyway Migrations:** V{version}__{description}.sql

**Documentação Técnica:**

**Spring Boot 4.x + Flyway:**
- [Flyway Migrations in Spring Boot 4.x | Medium](https://pranavkhodanpur.medium.com/flyway-migrations-in-spring-boot-4-x-what-changed-and-how-to-configure-it-correctly-dbe290fa4d47)

**JPA Relationships:**
- [Hibernate One-to-Many | Baeldung](https://www.baeldung.com/hibernate-one-to-many)
- [Best Practices Many-to-One | Thorben Janssen](https://thorben-janssen.com/best-practices-many-one-one-many-associations-mappings/)

## Dev Agent Record

### Agent Model Used

Story criada por: Claude Sonnet 4.5 (claude-sonnet-4-5-20250929)

### Debug Log References

N/A - Story em fase ready-for-dev

### Change Log

**2026-02-13 - Story 3.1 Implementation Complete**
- ✅ Criada migração V4__create_list_items.sql com tabela list_items
- ✅ Implementada entidade ListItem com campos dinâmicos (quantity, dueDate, url)
- ✅ Criado ListItemRepository com métodos: findByListIdOrderByPositionAsc, findByIdAndListId, findByIdWithDetails, countByListId
- ✅ Criados DTOs: ListItemResponseDTO, CreateItemRequestDTO, UpdateItemRequestDTO
- ✅ Criado ListItemMapper para conversão Entity <-> DTO
- ✅ Implementados 15 testes no ListItemRepositoryTest (todos passando)
- ✅ 154 testes totais do projeto passando (sem regressões)
- ⚠️ Migração renomeada de V3 para V4 (V3 já existia para list_members)

**2026-02-13 - Code Review Fixes Applied (AI)**
- ✅ Corrigido AC: V3__create_list_items.sql → V4__create_list_items.sql
- ✅ File List atualizado: Adicionado sprint-status.yaml (modificado)
- ✅ Melhorada documentação: ListItemMapper null handling clarificado
- ✅ Teste CASCADE DELETE refatorado: Usa SQL nativo para verificar CASCADE real
- ✅ 154 testes passando após correções (0 regressões)
- 📊 Issues corrigidos: 1 HIGH, 3 MEDIUM (5 LOW identificados para stories futuras)

**Arquivos Criados:**
1. `backend/src/main/resources/db/migration/V4__create_list_items.sql` - Migração Flyway
2. `backend/src/main/java/br/com/leoferolive/nossalista/listitem/domain/ListItem.java` - Entidade JPA
3. `backend/src/main/java/br/com/leoferolive/nossalista/listitem/repository/ListItemRepository.java` - Repository
4. `backend/src/main/java/br/com/leoferolive/nossalista/listitem/dto/ListItemResponseDTO.java` - DTO resposta
5. `backend/src/main/java/br/com/leoferolive/nossalista/listitem/dto/CreateItemRequestDTO.java` - DTO criação
6. `backend/src/main/java/br/com/leoferolive/nossalista/listitem/dto/UpdateItemRequestDTO.java` - DTO atualização
7. `backend/src/main/java/br/com/leoferolive/nossalista/listitem/dto/ListItemMapper.java` - Mapper
8. `backend/src/test/java/br/com/leoferolive/nossalista/listitem/repository/ListItemRepositoryTest.java` - Testes

### Completion Notes List

**Implementation Notes:**

Esta story foi criada com análise de:
- ✅ Epic 3 completo (6 stories)
- ✅ Story 2.1 (modelagem de dados - padrão de referência)
- ✅ Story 2.6 (última story completa - padrões atualizados)
- ✅ Architecture.md (Decision #002 - campos dinâmicos)

**Contexto Incorporado:**
- Padrão de modelagem de dados do Story 2.1
- Convenções de nomenclatura estabelecidas
- PostgreSQL via Docker Compose (não H2 em dev)
- @ManyToOne unidirecional com LAZY fetch
- CASCADE via database constraints
- Campos dinâmicos: quantity, due_date, url (nullable)

**Dev-Ready Guardrails:**
- Schema SQL completo especificado
- Entidade JPA com código exemplo
- Repository com queries necessárias
- DTOs e Mapper especificados
- Testes detalhados
- Armadilhas comuns documentadas

### File List

**Arquivos Criados:**

```
backend/src/main/java/br/com/leoferolive/nossalista/listitem/domain/ListItem.java
backend/src/main/java/br/com/leoferolive/nossalista/listitem/repository/ListItemRepository.java
backend/src/main/java/br/com/leoferolive/nossalista/listitem/dto/ListItemResponseDTO.java
backend/src/main/java/br/com/leoferolive/nossalista/listitem/dto/CreateItemRequestDTO.java
backend/src/main/java/br/com/leoferolive/nossalista/listitem/dto/UpdateItemRequestDTO.java
backend/src/main/java/br/com/leoferolive/nossalista/listitem/dto/ListItemMapper.java
backend/src/main/resources/db/migration/V4__create_list_items.sql
backend/src/test/java/br/com/leoferolive/nossalista/listitem/repository/ListItemRepositoryTest.java
```

**Arquivos Modificados (infraestrutura):**

```
_bmad-output/implementation-artifacts/sprint-status.yaml
```

**Arquivos Referenciados (não modificados):**

```
backend/src/main/java/br/com/leoferolive/nossalista/list/domain/List.java
backend/src/main/java/br/com/leoferolive/nossalista/list/repository/ListRepository.java
backend/src/main/java/br/com/leoferolive/nossalista/user/domain/User.java
backend/src/main/java/br/com/leoferolive/nossalista/user/repository/UserRepository.java
```

---

**Story Status:** ready-for-dev ✅

**Ultimate context engine analysis completed - comprehensive developer guide created**
