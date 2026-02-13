# Retrospectiva - Epic 2: Gestão de Listas Pessoais

**Data:** 2026-02-13
**Status:** ✅ COMPLETO
**Participantes:** Leo (Project Lead), Alice (PO), Charlie (Senior Dev), Dana (QA), Elena (Junior Dev), Bob (Scrum Master)

---

## 📊 Resumo do Epic

| Métrica | Valor |
|---------|-------|
| Stories Completadas | 6/6 (100%) |
| Testes Automatizados | ~130+ backend, ~48 frontend |
| Issues em Code Review | Múltiplos (todos resolvidos) |
| Status Final | ✅ COMPLETO |

### Stories Entregues

- ✅ **2.1**: Modelagem de Dados de Listas e Tipos
- ✅ **2.2**: Criar Nova Lista
- ✅ **2.3**: Listar Todas as Listas do Usuário
- ✅ **2.4**: Ver Detalhes de uma Lista
- ✅ **2.5**: Editar Nome da Lista
- ✅ **2.6**: Excluir Lista

---

## 💬 Check-in: Uma Palavra

| Participante | Palavra | Justificativa |
|--------------|---------|---------------|
| Bob (SM) | Consistente | Padrões do Epic 1 mantidos |
| Alice (PO) | Fluido | Entrega contínua sem bloqueios |
| Charlie (Dev) | Reusável | Componentes e padrões reutilizados |
| Dana (QA) | Testado | Cobertura de testes aumentando |
| Elena (Dev) | Estruturado | Dev Notes facilitaram aprendizado |
| Leo (Lead) | Consolidado | Fundação sólida para próximos epics |

---

## ✅ CONTINUE - O que devemos continuar fazendo

1. **Manter consistência com padrões estabelecidos**
   - DTOs com Records Java
   - RFC 7807 Problem Details para erros
   - SpringDoc para documentação
   - Reuso de hooks e componentes frontend

2. **Dev Notes detalhadas em cada story**
   - Documentação de aprendizados
   - Referências a stories anteriores
   - Patterns e convenções explícitas

3. **Code reviews rigorosos**
   - Encontraram issues críticas (LazyInitializationException, validações)
   - Melhoraram qualidade antes do merge

4. **Testes automatizados desde o início**
   - Cobertura crescente a cada story
   - Testes de integração verificando comportamentos reais

---

## 🚀 START - O que devemos começar a fazer

1. **Criar guia de padrões de componentes UI**
   - Padrões de modal (edição, exclusão, confirmação)
   - Formulários e validações
   - Estados de loading e erro

2. **Adicionar testes de integração frontend-backend**
   - Verificar race conditions
   - Testar fluxos E2E críticos
   - Automatizar na pipeline

3. **Documentar diagramas ER das entidades**
   - Facilitar onboarding de novos devs
   - Visualizar relacionamentos complexos

4. **Definir métricas de produto**
   - Quantas listas usuários criam
   - Quais tipos são mais usados
   - Taxa de retenção

5. **Implementar logging de auditoria**
   - Operações destrutivas (delete)
   - Mudanças de permissões
   - Ações críticas de segurança

---

## 🛑 STOP - O que devemos parar de fazer

1. **Assumir que frontend lida com nulls**
   - Sempre verificar contratos API
   - Usar fallbacks ou valores padrão
   - Documentar campos nullable

2. **Deixar testes manuais pendentes**
   - Resolver ou remover checkboxes
   - Não acumular dívida técnica
   - Automatizar quando possível

3. **Mudar requisitos durante implementação**
   - Definir comportamentos de erro claramente
   - Evitar discussões técnicas no meio do desenvolvimento
   - Documentar decisões de produto antes do dev

4. **Criar componentes sem testes**
   - Escrever testes junto com implementação
   - Não deixar para "depois"

---

## 📋 Lições Técnicas do Epic

### Patterns que Funcionaram

1. **findByIdWithDetails() para evitar LazyInitializationException**
   - Problema descoberto na 2.5, aplicado corretamente na 2.6
   - JOIN FETCH para relacionamentos LAZY

2. **Validação pós-trim**
   - @Size valida antes do trim
   - Validar manualmente no service após trim

3. **@Transactional obrigatório**
   - Previne race conditions (TOCTOU)
   - Garante atomicidade

4. **CASCADE via database constraints**
   - Não deletar manualmente itens/membros
   - Confie nas FK constraints

### Issues Recorrentes em Code Review

| Issue | Story | Correção |
|-------|-------|----------|
| LazyInitializationException | 2.5 | Usar findByIdWithDetails() |
| Falta de @Transactional | 2.5 | Adicionar anotação |
| Validação pré-trim | 2.5 | Validar após trim no service |
| Toast de loading | 2.5 | Adicionar "Atualizando..." |
| Modal não fecha em 403 | 2.5 | Fechar modal em erro de permissão |
| Focus trap em modais | 2.6 | Implementar acessibilidade |
| Testes de CASCADE | 2.6 | Verificar deleção em cascata |

---

## 🎯 Próximo Epic: Gestão de Itens

### Stories Planejadas

- **3.1**: Modelagem de Dados de Itens
- **3.2**: Adicionar Item à Lista
- **3.3**: Listar Itens de uma Lista
- **3.4**: Marcar/Desmarcar Item como Concluído
- **3.5**: Editar Item
- **3.6**: Remover Item

### Dependências do Epic 2

✅ **Fundação estável:**
- Entidade List e repositórios
- ListView page com layout definido
- Hooks useLists estabilizados
- Padrões de modal e formulários
- Sistema de Toast notifications

### Preparações Identificadas

| Preparação | Status | Notas |
|------------|--------|-------|
| Modelagem de list_items | Pendente | Campos dinâmicos por tipo |
| Atualizar ListView | Pendente | Adicionar seção de itens |
| Campo de adição no bottom | Pendente | Requisito crítico UX |
| Suporte a campos dinâmicos | Pendente | quantity, due_date, url |

---

## 📝 Ações do Epic 3

| Prioridade | Ação | Responsável | Critério de Sucesso |
|------------|------|-------------|---------------------|
| ALTA | Criar guia de padrões UI | Charlie | Documento em docs/ui-patterns.md |
| ALTA | Implementar modelagem de itens | Charlie | Migration V3 e entidades |
| ALTA | Garantir campo de adição sempre visível | Alice+Charlie | UX validado antes do dev |
| MÉDIA | Adicionar testes E2E | Dana | 3 fluxos críticos cobertos |
| MÉDIA | Documentar diagrama ER | Elena | Diagrama em docs/entities.md |
| MÉDIA | Definir métricas de produto | Alice | Métricas no dashboard |

---

## 🙏 Agradecimentos

- **Leo** pela liderança e visão técnica consistente
- **Alice** pela clareza nos requisitos de produto
- **Charlie** pela excelência técnica e revisões detalhadas
- **Dana** pela insistência em qualidade e testes
- **Elena** pelas perguntas que melhoraram documentação
- **Bob** por facilitar com foco em sistemas

---

## 📁 Referências

- [Epic 1 Retrospective](./retrospective-epic-1.md)
- [Epic 2 Stories](../planning-artifacts/epics.md#epic-2)
- [Story 2.1](./2-1-modelagem-de-dados-de-listas-e-tipos.md)
- [Story 2.2](./2-2-criar-nova-lista.md)
- [Story 2.3](./2-3-listar-todas-as-listas-do-usuario.md)
- [Story 2.4](./2-4-ver-detalhes-de-uma-lista.md)
- [Story 2.5](./2-5-editar-nome-da-lista.md)
- [Story 2.6](./2-6-excluir-lista.md)

---

**Status do Epic 2:** ✅ COMPLETO

**Próximo Epic:** Epic 3 - Gestão de Itens (preparação em andamento)
