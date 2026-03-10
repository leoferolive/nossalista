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
npm run test:e2e
```

## Seguranca e compliance (local)

```bash
cd frontend
npm audit --audit-level=high --omit=dev
npx --yes license-checker --production --failOn 'GPL;AGPL;LGPL'
```

- O gitleaks usa `.gitleaks.toml` para ignorar apenas artefatos internos gerados em `_bmad/` e `_bmad-output/`.

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
  - push em `main` roda `CI`
  - `release.yml` cria/reutiliza a tag semantica `vX.Y.Z`
  - `deploy-on-tag.yml` reconstrói o SHA aprovado e implanta a mesma tag em `nossalista-dev`
  - `deploy-branch-dev.yml` e o caminho manual para branches/SHAs nao mergeados; ele gera `vX.Y.Z-rc.<sha>` e implanta essa RC
- Prod:
  - `deploy-prod.yml` promove uma tag estavel existente `vX.Y.Z`
  - o job de deploy exige aprovacao no environment `production`
- Semantica operacional:
  - `tag` = tag da imagem implantada
  - `ref` = ref do checkout usado para build
  - o workflow sempre aplica o manifesto e depois executa `kubectl set image`, entao `latest` e `:dev` nao sao mais fonte de verdade

## Rollback de Produção por Tag

1. Identificar a tag estável anterior:

```bash
gh release list --limit 20
```

2. Reexecutar `deploy-prod.yml` apontando `tag` para a release estavel desejada.

3. Verificar rollout:

```bash
kubectl rollout status deployment/nossalista -n nossalista
kubectl get pods -n nossalista
kubectl get deployment nossalista -n nossalista -o jsonpath='{.metadata.annotations.deploy\.nossalista/tag}{"\n"}'
curl https://nossalista.leoferolive.com.br/api/health
```
