# Retrospectiva - Epic 1: Autenticação e Perfis de Usuário

**Data:** 2026-02-12
**Duração do Epic:** 11/02/2026 - 12/02/2026 (2 dias)
**Participantes:** Leo (Project Lead), Alice (PO), Charlie (Senior Dev), Dana (QA), Elena (Junior Dev)
**Facilitador:** Bob (Scrum Master)

---

## 📊 Resumo do Epic

| Métrica | Valor |
|---------|-------|
| Stories Completadas | 5/5 (100%) |
| Subtasks Executadas | ~140 tarefas |
| Testes Automatizados | 47 testes passando |
| Issues em Code Review | 32 issues (10 CRITICAL, 15 HIGH/MEDIUM, 7 LOW) |
| Status Final | ✅ COMPLETO |

---

## 📦 Deliverables

- ✅ Spring Boot 4.0.2 + Java 25
- ✅ PostgreSQL (dev/prod) + H2 (testes)
- ✅ JWT stateless (7 dias expiração)
- ✅ Google OAuth2 com PKCE
- ✅ BCrypt + RFC 7807 errors
- ✅ Swagger/OpenAPI documentation

---

## 💬 Check-in: Uma palavra

| Participante | Palavra |
|--------------|---------|
| Bob (SM) | Intenso |
| Alice (PO) | Fundação |
| Charlie (Dev) | Rigoroso |
| Dana (QA) | Validado |
| Elena (Dev) | Aprendizado |
| Leo (Lead) | Evolução |

---

## ✅ CONTINUE - O que devemos continuar fazendo

1. **Code reviews obrigatórios** - Encontrou issues críticas em todas as stories
2. **RFC 7807 Problem Details** - Padronizou respostas de erro da API
3. **Testes BDD (Given/When/Then)** - Facilita entendimento dos testes
4. **Javadoc em português** - Ajuda manutenção do código

---

## 🚀 START - O que devemos começar a fazer

1. **Planejar testes antes da implementação (TDD light)** - Evita surpresas com @Nested e outros problemas de infraestrutura
2. **Usar classes separadas para testes Spring** - Evitar @Nested em testes de integração
3. **Criar guia de padrões de código** - Acelera onboard de novos devs (constructor injection, records, conventions)
4. **Testar fluxos de erro além dos sucessos** - Prevenir edge cases inesperados

---

## 🛑 STOP - O que devemos parar de fazer

1. **Assumir que username derivado está disponível** - Implementar lógica incremental desde o início
2. **Hardcode URLs de frontend** - Usar environment variables desde Story 1.1
3. **Esquecer de validar nullable em entidades** - Verificar constraints de database nas migrations

---

## 📋 Ações para o Próximo Epic

| Prioridade | Ação | Dono | Epic Alvo |
|------------|------|------|-----------|
| ALTA | Code reviews obrigatórios para todas as stories | Charlie | Todos |
| ALTA | Planejar casos de teste antes da implementação | Dana | Epic 2+ |
| ALTA | Criar guia de padrões de código | Elena + Charlie | Epic 2 |
| MÉDIA | Evitar @Nested em testes de integração Spring | Dana | Todos |
| MÉDIA | Validar constraints de database nas migrations | Charlie | Todos |
| BAIXA | Revisar URLs hardcoded vs environment variables | Elena | Epic 2 |

---

## 📝 Lições Técnicas

### Padrões que funcionaram

- **Constructor Injection** sem @Autowired (Spring 5+)
- **Records Java** para DTOs imutáveis
- **@Transactional(readOnly = true)** para queries
- **Normalização de inputs** (trim, toLowerCase) antes de buscar no database
- **Reutilização de serviços** (JwtService compartilhado entre email/senha e OAuth2)

### Problemas e soluções

- **Story 1.2:** Campo `role` faltando → Adicionado via code review
- **Story 1.3:** Normalização de username → Adicionado toLowerCase() + @Pattern
- **Story 1.4:** Dependência circular → PasswordEncoder extraído para PasswordConfig.java
- **Story 1.5:** @Nested em testes Spring → Documentado como padrão a evitar

---

## 🎯 FRs Cobertos

- ✅ FR1: Registro de novo usuário (email/senha)
- ✅ FR2: Login com email/senha
- ✅ FR3: Login com Google OAuth2
- ✅ FR4: Acessar próprio perfil
- ✅ FR5: Atualizar informações do próprio perfil
- ✅ FR6: Buscar outros usuários por username
- ✅ FR7: Google OAuth2 como método preferencial

---

## 🙏 Agradecimentos

- Leo pela liderança técnica e confiança no processo
- Alice pela visão de produto clara
- Charlie pelas correções críticas e orientação técnica
- Dana pela insistência em qualidade de testes
- Elena pelas perguntas que trouxeram melhorias
- Bob por facilitação focada em sistemas, não pessoas

---

**Status do Epic 1:** ✅ COMPLETO

**Próximo Epic:** Epic 2 - Listas Compartilhadas (preparação pendente)
