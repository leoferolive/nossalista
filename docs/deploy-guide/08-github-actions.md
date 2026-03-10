# GitHub Actions - Release e Deploy

## 1. Visao geral

Workflows ativos no repositorio:

- `ci.yml`: quality gates (frontend, backend, seguranca, smoke).
- `release.yml`: roda apos CI com sucesso em `main`, cria/reutiliza tag estavel e GitHub Release, depois dispara deploy em dev por tag.
- `deploy-on-tag.yml`: deploy em dev para tag estavel (`vX.Y.Z`), com validacao de `tag` + `ref`.
- `deploy-branch-dev.yml`: deploy manual em dev para branch/SHA nao mergeado, criando RC tag rastreavel.
- `deploy-prod.yml`: deploy manual em prod com aprovacao de `environment: production`.
- `deploy-environment.yml`: workflow reutilizavel central com build/push + apply/rollout Kubernetes.

## 2. Fluxo oficial

```text
push main -> CI -> release.yml
                 -> cria/reusa tag estavel vX.Y.Z
                 -> gh workflow run deploy-on-tag.yml (tag + sha)
                 -> deploy em dev

manual -> deploy-branch-dev.yml (ref obrigatorio)
       -> cria RC tag vX.Y.Z-rc.<sha>
       -> deploy em dev

manual -> deploy-prod.yml (tag estavel)
       -> valida tag
       -> aprovacao production
       -> deploy em prod
```

## 3. Guardrails de seguranca operacional

- `deploy-branch-dev.yml`
  - input `ref` obrigatorio (sem default), para evitar deploy acidental de `main`.
  - RC tag e pre-release para rastreabilidade.
- `deploy-on-tag.yml`
  - falha se `tag` nao estiver em formato estavel `vX.Y.Z`.
  - falha se `tag` e `ref` nao apontarem para o mesmo commit.
- `deploy-prod.yml`
  - falha se `tag` nao estiver em formato estavel `vX.Y.Z`.
  - falha se a tag nao existir no repositorio antes da aprovacao.
- `deploy-environment.yml`
  - build usa SHA real do checkout (`git rev-parse HEAD`) no `GIT_SHA`.
  - publica `latest` por conveniencia operacional, mas implanta explicitamente `ghcr.io/<owner>/<app>:<tag>`.
  - grava annotations `deploy.nossalista/tag` e `deploy.nossalista/sha` no Deployment.
  - injeta `APP_VERSION`, `APP_GIT_TAG`, `APP_GIT_SHA`, `APP_BUILD_TIME` e `APP_ENVIRONMENT` no pod.

## 4. Ambientes e imagens

| Ambiente | Namespace K8s | Deployment | Imagem implantada |
| --- | --- | --- | --- |
| Dev | `nossalista-dev` | `nossalista-dev` | `ghcr.io/leoferolive/nossalista-dev:<tag-estavel-ou-rc>` |
| Prod | `nossalista` | `nossalista` | `ghcr.io/leoferolive/nossalista:<tag-estavel>` |

## 5. Rastreabilidade da versao implantada

- `tag`: tag da imagem publicada e implantada.
- `ref`: ref do checkout usado para reconstruir o codigo.
- O cluster deixa de depender de `:latest` ou `:dev` para decidir o que esta rodando.
- `GET /api/health` retorna `version`, `gitSha`, `gitTag`, `environment` e `buildTime`.

## 6. Comandos uteis de validacao

```bash
gh run list --workflow=deploy-branch-dev.yml --limit 5
gh run list --workflow=deploy-on-tag.yml --limit 5
gh run list --workflow=deploy-prod.yml --limit 5
gh release list --limit 10
gh release view vX.Y.Z

kubectl get deployment nossalista-dev -n nossalista-dev -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
kubectl get deployment nossalista -n nossalista -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'
curl http://nossalista.home/api/health
curl https://nossalista.leoferolive.com.br/api/health
```
