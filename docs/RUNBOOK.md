# Runbook Operacional

## Subir ambiente local

1. Banco:

```bash
docker compose up -d
```

2. Backend:

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

3. Frontend:

```bash
cd frontend
npm install
npm run dev
```

## Validacao rapida

- API health: `GET http://localhost:8080/api/health`
- Frontend: `http://localhost:5173`
- Auth UI: landing em `http://localhost:5173/?auth=login` (rota `/login` e legada e redireciona para `/`)
- Auditoria de versao implantada:
  - `curl http://nossalista.home/api/health`
  - `curl https://nossalista.leoferolive.com.br/api/health`
  - `kubectl get deployment nossalista-dev -n nossalista-dev -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'`
  - `kubectl get deployment nossalista -n nossalista -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'`

## Qualidade

Backend:

```bash
cd backend
./mvnw -B -Pstrict-quality -Ddependency-check.skip=true verify
./mvnw -B -Pregression-tests test
./mvnw -B -DskipTests package
java -jar target/nossalista-0.0.1-SNAPSHOT.jar --spring.profiles.active=ci
```

Frontend:

```bash
cd frontend
npm run lint
npm run format:check
npm run stylelint
npm run typecheck
npm run test:coverage
npm run build
npm run bundle:check
npm run test:e2e:pr
npm run test:e2e:fullstack
```

Observacoes E2E:
- Suite de PR usa tag `@pr` e modo deterministic/mockado.
- Suite full-stack usa tag `@fullstack` e precisa do backend ativo em `http://127.0.0.1:8080`.

## Seguranca e compliance (local)

```bash
cd frontend
npm audit --audit-level=high --omit=dev
npx --yes license-checker --production --failOn 'GPL;AGPL;LGPL'
```

- O gitleaks usa `.gitleaks.toml` para ignorar apenas artefatos internos gerados em `_bmad/` e `_bmad-output/`.

## Banco de vulnerabilidades NVD (dependency-check na CI)

O gate `security-and-compliance` roda o OWASP dependency-check contra o banco NVD. Para as PRs de backend serem rápidas (<1 min), o banco é mantido "quente" num cache do `main` pelo workflow `nvd-cache-warmer.yml`; as PRs só leem esse cache e rodam com `-DautoUpdate=false` (não baixam a NVD). Ver **D-021** em `docs/DECISIONS.md`.

**Semear / atualizar o cache manualmente** (necessário 1x após o merge inicial, ou se o cache for *evicted*):

```bash
gh workflow run nvd-cache-warmer.yml
gh run watch   # acompanha (~15 min no 1o pull; incremental nos seguintes)
```

- O warmer também roda sozinho todo dia (cron 06:00 UTC) e em push que altere `backend/pom.xml`.
- Se uma PR de backend falhar com `Banco NVD ausente no cache (cache frio)`, o próprio job já disparou o warmer — basta reexecutar a PR após ~15 min.

## Operacao em Kubernetes

```bash
kubectl apply -f k8s/dev/
kubectl apply -f k8s/prod/
kubectl get pods -n nossalista-dev
kubectl get pods -n nossalista
kubectl logs -f deployment/nossalista-dev -n nossalista-dev
kubectl logs -f deployment/nossalista -n nossalista
kubectl rollout restart deployment/nossalista-dev -n nossalista-dev
kubectl rollout restart deployment/nossalista -n nossalista
```

## Release e Deploy (GitHub Actions)

- Dev:
  - `deploy-branch-dev.yml`: deploy manual de branch/SHA com RC tag rastreavel
  - `deploy-on-tag.yml`: deploy em dev de tag estavel `vX.Y.Z` (manual ou disparado por `release.yml`)
- Prod:
  - `deploy-prod.yml`: deploy manual de tag estavel `vX.Y.Z` com aprovacao no environment `production`
- Release:
  - push em `main` roda `CI`
  - `release.yml` cria/reutiliza tag patch `vX.Y.Z`, publica GitHub Release e dispara `deploy-on-tag.yml`

Regras operacionais:
- `deploy-branch-dev.yml`: `ref` obrigatorio (sem default)
- `deploy-on-tag.yml`: valida formato de tag estavel e alinhamento entre `tag` e `ref`
- `deploy-prod.yml`: aceita apenas tag estavel existente no repositorio
- `frontend-e2e-fullstack.yml`: executa E2E navegador↔backend **manualmente** via `workflow_dispatch`. O cron noturno (`06:00 UTC`) está **DESABILITADO** (comentado) por limite de billing do GitHub Actions — não reativar sem decisão sobre billing. O workflow já possui notificação de falha (`if: failure()` abre issue `ci-failure` via `gh`) e cache dos browsers Playwright, prontos para quando rodar (manual hoje, ou cron se reativado)
- `deploy-environment.yml` aplica o manifesto e depois executa `kubectl set image`, entao `latest` e `:dev` nao sao mais fonte de verdade do que ficou implantado
- `tag` = tag da imagem implantada; `ref` = ref do checkout usado para build
- ao final de cada workflow de deploy, consultar a aba **Summary** do GitHub Actions para ver `Tag version deployada`, imagem, SHA e ambiente efetivos

## Rollback de Produção por Tag

1. Identificar a tag estável anterior:

```bash
gh release list --limit 20
```

2. Disparar o workflow dedicado de rollback apontando `tag` para a release estavel
   desejada (requer aprovação manual no environment `production`):

```bash
gh workflow run rollback-prod.yml --field tag=v1.2.2
```

   > `rollback-prod.yml` espelha o `deploy-prod.yml` (mesma validação de tag, mesma
   > aprovação e reuso do `deploy-environment.yml`); só muda a semântica para
   > reimplantar uma release já conhecida. Compartilha o `concurrency group`
   > `deploy-prod`, então não roda em paralelo com um deploy de prod.
   > Alternativa: reexecutar o próprio `deploy-prod.yml` com a tag anterior produz
   > o mesmo efeito.

3. Verificar rollout:

```bash
kubectl rollout status deployment/nossalista -n nossalista
kubectl get pods -n nossalista
kubectl get deployment nossalista -n nossalista -o jsonpath='{.metadata.annotations.deploy\.nossalista/tag}{"\n"}'
curl https://nossalista.leoferolive.com.br/api/health
```
