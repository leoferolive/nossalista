# GitHub Actions — Workflows de Release e Deploy

## 1. Visão Geral dos Workflows

O repositório usa cinco workflows de deploy/release e um workflow reutilizável:

- `ci.yml`: valida qualidade, testes, segurança e build.
- `release.yml`: após `CI` bem-sucedido em push para `main`, cria ou reutiliza a tag estável `vX.Y.Z`, publica GitHub Release e dispara deploy em `dev`.
- `deploy-on-tag.yml`: recebe uma `tag` estável e um `ref` de checkout, reconstrói o código correspondente e implanta essa mesma tag em `dev`.
- `deploy-branch-dev.yml`: gera uma RC rastreável (`vX.Y.Z-rc.<sha>`) para uma branch/SHA não mergeada e implanta essa RC em `dev`.
- `deploy-prod.yml`: promove uma tag estável existente para `prod` com aprovação no environment `production`.
- `deploy-environment.yml`: único workflow reutilizável com a lógica de build, push em GHCR e atualização do Deployment no cluster.

---

## 2. CI (`.github/workflows/ci.yml`)

### Triggers

- `pull_request`
- `push` em:
  - `main`

### Objetivo

Garantir que somente commits aprovados pelo quality gate avancem para release e deploy.

---

## 3. Semântica Operacional

- `tag`: tag da imagem que será publicada e implantada.
- `ref`: ref do checkout usado para reconstruir o código.
- O workflow reutilizável resolve `git rev-parse HEAD` a partir de `ref`, publica a imagem com labels OCI e aplica o Deployment com `kubectl set image`.
- O cluster passa a registrar também:
  - annotation `deploy.nossalista/tag`
  - annotation `deploy.nossalista/sha`
  - variáveis de ambiente `APP_VERSION`, `APP_GIT_TAG`, `APP_GIT_SHA`, `APP_BUILD_TIME` e `APP_ENVIRONMENT`

---

## 4. Deploy Dev por Release (`release.yml` -> `deploy-on-tag.yml`)

### Trigger

`workflow_run` do workflow `CI`, somente quando:

- branch do run: `main`
- conclusão: `success`
- evento original: `push`

### Etapas

1. Checkout no `head_sha` validado pelo CI.
2. Resolver versão de release:
   - se o commit já tiver tag `vX.Y.Z`, reutiliza;
   - senão, lê a última tag SemVer estável e incrementa patch.
3. Criar tag Git anotada (`vX.Y.Z`) e publicar.
4. Criar GitHub Release com `--generate-notes` (se ainda não existir).
5. Disparar `deploy-on-tag.yml` com:
   - `tag = vX.Y.Z`
   - `ref = SHA aprovado`
6. `deploy-environment.yml` reconstrói esse SHA e implanta `ghcr.io/leoferolive/nossalista-dev:vX.Y.Z`.

### Resultado

- `dev` recebe exatamente o commit aprovado no CI de `main`.
- A imagem implantada, a tag Git e o `/api/health` passam a compartilhar os mesmos metadados de versão.

---

## 5. Deploy Dev por RC (`deploy-branch-dev.yml`)

### Trigger

`workflow_dispatch`

### Etapas

1. Receber um `ref` manual (branch, tag ou SHA).
2. Resolver o SHA exato do checkout.
3. Gerar RC tag `vX.Y.Z-rc.<sha-curto>`.
4. Criar pre-release no GitHub.
5. Implantar `ghcr.io/leoferolive/nossalista-dev:<rc_tag>` em `dev`.

### Resultado

- O ambiente `dev` pode validar código ainda não mergeado sem perder rastreabilidade.

---

## 6. Deploy Prod (`deploy-prod.yml`)

### Trigger

`workflow_dispatch`

### Etapas

1. Receber uma tag estável existente (`vX.Y.Z`).
2. Aguardar aprovação manual no environment `production`.
3. `deploy-environment.yml` faz checkout da própria tag.
4. A imagem `ghcr.io/leoferolive/nossalista:vX.Y.Z` é reconstruída e implantada em `prod`.

### Resultado

- Produção não depende de `latest` como fonte de verdade.
- Toda entrega em prod fica rastreável por:
  - tag Git (`vX.Y.Z`)
  - GitHub Release
  - imagem implantada com a mesma tag
  - annotations `deploy.nossalista/tag` e `deploy.nossalista/sha`
  - resposta de `GET /api/health`

---

## 7. Verificação Rápida via GitHub CLI

```bash
gh run list --workflow=ci.yml --limit 5
gh run list --workflow=release.yml --limit 5
gh run list --workflow=deploy-on-tag.yml --limit 5
gh run list --workflow=deploy-branch-dev.yml --limit 5
gh run list --workflow=deploy-prod.yml --limit 5

gh release list --limit 10
gh release view vX.Y.Z

curl http://nossalista.home/api/health
kubectl get deployment nossalista-dev -n nossalista-dev -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
```
