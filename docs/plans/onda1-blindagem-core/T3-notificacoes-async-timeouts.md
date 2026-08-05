# T3 · Notificações fora da transação + timeouts

**Branch:** `feat/onda1-t3-notificacoes-async` · **Origem:** P1 (chamadas externas síncronas em transação, sem timeout)

## Problema

- `ListItemService.addItem` chama `notificationService.notifyListMembers` **ainda dentro** do
  método `@Transactional`; o push/broadcast é disparado **antes do commit** (pode notificar uma
  mudança que sofre rollback) e mantém a transação DB aberta durante I/O externo.
- `PushNotificationService.sendToUser` e `SmtpEmailService.sendHtmlEmail` são **bloqueantes** e
  rodam na thread da request; SMTP sem timeout = JavaMail com timeout **infinito** (thread travada).

## Objetivo

Tirar push/email/broadcast do caminho transacional e da thread da request (executar **após o
commit**, de forma **assíncrona**), e configurar **timeouts explícitos** para SMTP e push.

## Escopo (arquivos que esta tarefa PODE tocar)

- `backend/src/main/java/br/com/leoferolive/nossalista/listitem/service/ListItemService.java`
- `backend/src/main/java/br/com/leoferolive/nossalista/notification/**`
- `backend/src/main/java/br/com/leoferolive/nossalista/push/PushNotificationService.java`
- `backend/src/main/java/br/com/leoferolive/nossalista/email/service/SmtpEmailService.java`
- `backend/src/main/java/br/com/leoferolive/nossalista/config/` — novo `AsyncConfig.java`
- `backend/src/main/resources/application.yml`
- `backend/src/test/java/.../` — testes novos/ajustados

**NÃO** editar as entidades `ListItem`/`SharedList`, `GlobalExceptionHandler` (T1), nem
`PushSubscriptionStore`/entity/repository de push (T2), nem `pom.xml`. Mantenha o consumo da
API pública do `PushSubscriptionStore` como está.

## Passos

1. Criar `config/AsyncConfig.java` com `@EnableAsync` e um `ThreadPoolTaskExecutor` **bounded**
   (core/max pool e queue capacity razoáveis; nome de thread claro; política de rejeição sensata).
2. Mover o disparo de notificação/broadcast para **após o commit**. Abordagem idiomática recomendada:
   publicar um `ApplicationEvent` dentro do serviço e consumir com
   `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async`. Assim nada é notificado se a
   transação der rollback, e o I/O sai da thread da request. (Alternativa: `TransactionSynchronization`.)
3. Anotar `SmtpEmailService.sendHtmlEmail` e `PushNotificationService.sendToUser` (ou o listener que
   os invoca) como `@Async` no executor dedicado. Garantir que exceções assíncronas sejam **logadas**
   e não derrubem nada (elas já não devem quebrar a request).
4. `application.yml`: adicionar `spring.mail.properties.mail.smtp.connectiontimeout`, `.timeout`,
   `.writetimeout` (valores explícitos, ex.: 5–10s). Se o `PushService`/web-push permitir timeout,
   configurá-lo; senão, registrar no PR que a lib não expõe timeout configurável.
5. Testes: comprovar que (a) a notificação só ocorre após commit (não ocorre em rollback), e
   (b) o caminho de notificação é assíncrono (não bloqueia o retorno da operação de item).
   Ajustar/rever testes existentes de `ListItemService`/`NotificationService` que dependiam do
   disparo síncrono.

## Critérios de aceite

- [ ] `@EnableAsync` + executor bounded configurado (`AsyncConfig`).
- [ ] Notificações/push/broadcast disparam **após o commit** (evento AFTER_COMMIT) e **fora** da thread da request.
- [ ] Timeouts SMTP explícitos no `application.yml`; timeout de push configurado ou limitação registrada.
- [ ] Testes provando: sem notificação em rollback; caminho assíncrono; nenhuma regressão nos testes de item.
- [ ] `./scripts/quality.sh --pre-commit` verde.
- [ ] Documentação impactada atualizada (governança do `CLAUDE.md`): registrar a mudança de fluxo
      (síncrono → AFTER_COMMIT/@Async) em `docs/DECISIONS.md`.

## Notas

- Cuidado para **não** alterar a semântica de ordenação percebida pelo cliente (o `revision` já
  ordena no front). O broadcast deve continuar consistente por lista.
- Esta é a tarefa mais sensível da onda (toca o caminho de escrita + realtime). Testes e review
  devem focar em não introduzir regressão em tempo real.

## Restrições de commit

- **Sem** atribuição de IA nos commits (regra do `CLAUDE.md`). Mensagens em PT-BR.
