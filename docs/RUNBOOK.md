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
kubectl apply -f k8s/
kubectl get pods -n nossalista
kubectl logs -f deployment/nossalista -n nossalista
kubectl rollout restart deployment/nossalista -n nossalista
```

## Observacoes atuais

- O workflow `.github/workflows/deploy.yml` esta temporariamente desativado.
- O deploy automatizado deve ser reativado apenas apos correcao do pipeline externo.
