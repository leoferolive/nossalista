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
./mvnw -B verify
./mvnw -B -Pstrict-quality verify
./mvnw -B -Pregression-tests test
```

Frontend:

```bash
cd frontend
npm run test -- --run
npm run build
```

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
