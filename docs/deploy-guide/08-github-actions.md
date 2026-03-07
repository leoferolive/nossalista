# GitHub Actions — Workflows de Deploy

## 1. Atualização Necessária no self-workflows

**Problema:** O workflow reutilizável `self-workflows/.github/workflows/deploy.yml` sempre usa a tag `:latest`. Para dois ambientes no mesmo repositório, precisamos de tags distintas (`:dev` e `:latest`).

**Ação:** No repositório `leoferolive/self-workflows`, adicionar o input `image_tag`:

### 1.1 Adicionar input ao workflow reutilizável

```yaml
# self-workflows/.github/workflows/deploy.yml
# Adicionar em: on.workflow_call.inputs

image_tag:
  required: false
  type: string
  default: 'latest'
  description: 'Tag da imagem Docker (ex: latest, dev, staging)'
```

### 1.2 Usar o input no step de build e push

```yaml
# No step de build da imagem Docker, substituir tags hardcoded por:
tags: |
  ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ inputs.image_tag }}
  ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }}
```

### 1.3 Usar o input no step de kubectl apply

```yaml
# Se o self-workflow faz substituição de tag nos manifests:
image: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ inputs.image_tag }}
```

---

## 2. .github/workflows/deploy-dev.yml (Novo Arquivo)

```yaml
name: Deploy Dev (nossalista.home)

on:
  workflow_run:
    workflows: [CI]
    branches: [main]
    types: [completed]

jobs:
  deploy:
    if: ${{ github.event.workflow_run.conclusion == 'success' }}
    permissions:
      contents: read
      packages: write
    uses: leoferolive/self-workflows/.github/workflows/deploy.yml@main
    with:
      app_name: nossalista-dev
      app_namespace: nossalista-dev
      app_type: backend
      dockerfile_path: ./Dockerfile
      k8s_path: ./k8s/dev
      image_tag: dev
    secrets: inherit
```

---

## 3. .github/workflows/deploy-prod.yml (Novo Arquivo)

```yaml
name: Deploy Prod (nossalista.leoferolive.com.br)

on:
  workflow_dispatch:
    inputs:
      confirm:
        description: "Digite 'deploy' para confirmar o deploy em produção"
        required: true
        type: string

jobs:
  check-confirm:
    runs-on: ubuntu-latest
    steps:
      - name: Verificar confirmação
        if: ${{ inputs.confirm != 'deploy' }}
        run: |
          echo "Confirmação inválida. Digite 'deploy' para prosseguir."
          exit 1

  deploy:
    needs: check-confirm
    permissions:
      contents: read
      packages: write
    uses: leoferolive/self-workflows/.github/workflows/deploy.yml@main
    with:
      app_name: nossalista
      app_namespace: nossalista
      app_type: backend
      dockerfile_path: ./Dockerfile
      k8s_path: ./k8s/prod
      image_tag: latest
    secrets: inherit
```

---

## 4. deploy.yml Atual — Desativar

O arquivo `.github/workflows/deploy.yml` existente está comentado. Após os novos workflows estarem validados, pode ser removido:

```bash
# Remover o arquivo antigo
rm .github/workflows/deploy.yml
git add -A
git commit -m "Remove deploy.yml legado (substituído por deploy-dev.yml e deploy-prod.yml)"
```

---

## 5. ci.yml — Sem Mudanças Necessárias

O `ci.yml` roda testes para frontend e backend em paralelo a cada push/PR e permanece ativo.

---

## 6. Fluxo Completo de Deploy

### Deploy Dev (automático)

```
git push main
  → ci.yml (testes) — paralelo com deploy-dev.yml
  → deploy-dev.yml:
      1. Conecta ao homelab via Tailscale
      2. docker buildx build --platform linux/arm64
      3. docker push ghcr.io/leoferolive/nossalista:dev
      4. kubectl apply -f k8s/dev/
      5. kubectl rollout status deployment/nossalista-dev -n nossalista-dev
```

### Deploy Prod (manual)

```
GitHub → Actions → "Deploy Prod" → Run workflow → confirm: "deploy"
  → check-confirm (valida input)
  → deploy:
      1. Conecta ao homelab via Tailscale
      2. docker buildx build --platform linux/arm64
      3. docker push ghcr.io/leoferolive/nossalista:latest
      4. kubectl apply -f k8s/prod/
      5. kubectl rollout status deployment/nossalista -n nossalista
```

---

## 7. Verificar Status dos Workflows

```bash
# Via GitHub CLI
gh run list --workflow=deploy-dev.yml --limit 5
gh run list --workflow=deploy-prod.yml --limit 5

# Ver logs de um run específico
gh run view <run-id> --log
```
