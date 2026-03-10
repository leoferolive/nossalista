# GitHub Actions — Workflows de Release e Deploy

## 1. Visão Geral dos Workflows

O repositório usa três workflows principais:

- `ci.yml`: valida qualidade, testes, segurança e build.
- `deploy-dev.yml`: deploy automático no ambiente dev após CI com sucesso em `release/*`.
- `release-prod.yml`: gera release automática (`vX.Y.Z` patch), cria GitHub Release e faz deploy em prod com aprovação de environment.

---

## 2. CI (`.github/workflows/ci.yml`)

### Triggers

- `pull_request`
- `push` em:
  - `main`
  - `release/**`

### Objetivo

Garantir que somente commits aprovados pelo quality gate avancem para deploy/release.

---

## 3. Deploy Dev (`.github/workflows/deploy-dev.yml`)

### Trigger

`workflow_run` do workflow `CI`, somente quando:

- branch do run: `release/**`
- conclusão: `success`
- evento original: `push`

### Resultado

- Build e push da imagem `ghcr.io/leoferolive/nossalista-dev:latest`
- Apply em `k8s/dev`
- Rollout de `nossalista-dev` no namespace `nossalista-dev`

### Observação

O ambiente dev é único. A última branch `release/*` com CI aprovado sobrescreve o deploy anterior.

---

## 4. Release + Deploy Prod (`.github/workflows/release-prod.yml`)

### Trigger

`workflow_run` do workflow `CI`, somente quando:

- branch do run: `main`
- conclusão: `success`
- evento original: `push`

### Etapas

1. Checkout no `head_sha` validado pelo CI.
2. Resolver versão de release:
   - se o commit já tiver tag `vX.Y.Z`, reutiliza;
   - senão, lê última tag SemVer e incrementa patch.
3. Criar tag Git anotada (`vX.Y.Z`) e publicar.
4. Criar GitHub Release com `--generate-notes` (se ainda não existir).
5. Aguardar aprovação manual no environment `production`.
6. Build/push + deploy em `k8s/prod` usando `image_tag` igual à tag da release.

### Resultado

- Produção deixa de ser controlada por `latest` como versão operacional.
- Toda entrega em prod fica rastreável por:
  - tag Git (`vX.Y.Z`)
  - GitHub Release
  - imagem implantada com a mesma tag

---

## 5. Workflow Prod Legado

O workflow manual antigo `deploy-prod.yml` foi removido para evitar caminhos paralelos sem governança de release.

---

## 6. Verificação Rápida via GitHub CLI

```bash
gh run list --workflow=ci.yml --limit 5
gh run list --workflow=deploy-dev.yml --limit 5
gh run list --workflow=release-prod.yml --limit 5

gh release list --limit 10
gh release view vX.Y.Z
```
