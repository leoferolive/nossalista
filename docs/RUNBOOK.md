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
  - push em `release/*` roda `CI`
  - `deploy-dev.yml` publica `:dev` no namespace `nossalista-dev`
- Prod:
  - push em `main` roda `CI`
  - `release-prod.yml` cria tag patch `vX.Y.Z` e GitHub Release
  - job de deploy exige aprovação no environment `production`

## Rollback de Produção por Tag

1. Identificar a tag estável anterior:

```bash
gh release list --limit 20
```

2. Reexecutar deploy de produção apontando `image_tag` para a tag alvo (via workflow `release-prod.yml` ajustado para o commit/tag, ou via procedimento operacional de emergência no cluster).

3. Verificar rollout:

```bash
kubectl rollout status deployment/nossalista -n nossalista
kubectl get pods -n nossalista
```
