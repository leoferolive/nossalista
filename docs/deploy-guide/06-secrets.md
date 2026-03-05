# Secrets Kubernetes

## 1. ghcr-secret (Registry Pull Secret)

O secret `ghcr-secret` já existe no namespace `demo-api`. Precisa ser replicado para os namespaces `nossalista-dev` e `nossalista`.

```bash
# Garantir que os namespaces existem antes de copiar
kubectl apply -f k8s/dev/namespace.yaml
kubectl apply -f k8s/prod/namespace.yaml

# Copiar ghcr-secret para nossalista-dev
kubectl get secret ghcr-secret -n demo-api -o yaml \
  | sed 's/namespace: demo-api/namespace: nossalista-dev/' \
  | kubectl apply -f -

# Copiar ghcr-secret para nossalista
kubectl get secret ghcr-secret -n demo-api -o yaml \
  | sed 's/namespace: demo-api/namespace: nossalista/' \
  | kubectl apply -f -

# Verificar
kubectl get secret ghcr-secret -n nossalista-dev
kubectl get secret ghcr-secret -n nossalista
```

---

## 2. nossalista-secrets (Variáveis da Aplicação)

O deployment usa `envFrom.secretRef` para injetar todas as variáveis do secret como env vars no container.

### 2.1 Criar para nossalista-dev

```bash
kubectl create secret generic nossalista-secrets \
  -n nossalista-dev \
  --from-literal=SPRING_PROFILES_ACTIVE=dev \
  --from-literal=DATABASE_URL=jdbc:postgresql://postgres.database.svc.cluster.local:5432/nossalista_dev \
  --from-literal=DATABASE_USER=nossalista_dev \
  --from-literal=DATABASE_PASSWORD=<senha-dev> \
  --from-literal=JWT_SECRET=<secret-min-32-chars-dev> \
  --from-literal=GOOGLE_CLIENT_ID=<google-client-id> \
  --from-literal=GOOGLE_CLIENT_SECRET=<google-client-secret> \
  --from-literal=FRONTEND_URL=http://nossalista.home
```

### 2.2 Criar para nossalista (prod)

```bash
kubectl create secret generic nossalista-secrets \
  -n nossalista \
  --from-literal=SPRING_PROFILES_ACTIVE=prod \
  --from-literal=DATABASE_URL=jdbc:postgresql://postgres.database.svc.cluster.local:5432/nossalista \
  --from-literal=DATABASE_USER=nossalista \
  --from-literal=DATABASE_PASSWORD=<senha-prod> \
  --from-literal=JWT_SECRET=<secret-min-32-chars-prod> \
  --from-literal=GOOGLE_CLIENT_ID=<google-client-id> \
  --from-literal=GOOGLE_CLIENT_SECRET=<google-client-secret> \
  --from-literal=FRONTEND_URL=https://nossalista.leoferolive.com.br
```

> **Nota:** O mesmo `GOOGLE_CLIENT_ID` e `GOOGLE_CLIENT_SECRET` pode ser usado em ambos os ambientes, desde que os redirect URIs estejam cadastrados no Google Cloud Console (ver [12-google-oauth.md](12-google-oauth.md)).

### 2.3 Atualizar secret existente

Se o secret já existe e precisa ser atualizado:

```bash
# Deletar e recriar (mais simples)
kubectl delete secret nossalista-secrets -n nossalista-dev
kubectl create secret generic nossalista-secrets -n nossalista-dev ...

# Ou editar diretamente
kubectl edit secret nossalista-secrets -n nossalista-dev
# Os valores são base64 — usar: echo -n "valor" | base64
```

---

## 3. Referência de Variáveis

| Variável | Descrição | Dev | Prod |
|----------|-----------|-----|------|
| `SPRING_PROFILES_ACTIVE` | Profile Spring Boot | `dev` | `prod` |
| `DATABASE_URL` | JDBC URL do PostgreSQL | `...nossalista_dev` | `...nossalista` |
| `DATABASE_USER` | Usuário do banco | `nossalista_dev` | `nossalista` |
| `DATABASE_PASSWORD` | Senha do banco | gerada | gerada |
| `JWT_SECRET` | Secret para assinar JWT (mín. 32 chars) | gerado | gerado diferente |
| `GOOGLE_CLIENT_ID` | Client ID OAuth Google | compartilhado | compartilhado |
| `GOOGLE_CLIENT_SECRET` | Client Secret OAuth Google | compartilhado | compartilhado |
| `FRONTEND_URL` | URL base do frontend (CORS + OAuth redirect) | `http://nossalista.home` | `https://nossalista.leoferolive.com.br` |

### Gerar JWT_SECRET seguro

```bash
# Gerar string aleatória de 64 chars (adequada para HMAC-SHA256)
openssl rand -base64 48
```

---

## 4. Verificação

```bash
# Listar secrets nos namespaces
kubectl get secrets -n nossalista-dev
kubectl get secrets -n nossalista

# Verificar que o deployment consegue ler o secret
kubectl describe deployment nossalista-dev -n nossalista-dev | grep -A5 "Environment"

# Ver eventos do pod (útil para diagnosticar secret não encontrado)
kubectl describe pod -l app=nossalista-dev -n nossalista-dev | grep -A10 Events
```
