# TASKS — Lista Mestre de Execução

Lista consolidada de todas as tarefas para configurar os dois ambientes de deploy da NossaLista.
Marcar `[x]` ao concluir cada item.

---

## Fase 0 — Preparação

- [x] Verificar acesso kubectl ao cluster (`kubectl get nodes`)
- [ ] Verificar Tailscale no servidor k3s (`tailscale status`)
- [x] **Investigar exposição do Traefik para rede local** — ver [10-dns.md](10-dns.md) seção 1
  - `kubectl get service traefik -n traefik-system`
  - `kubectl describe daemonset traefik -n traefik-system`
  - Documentar resultado: ClusterIP / NodePort / LoadBalancer / hostNetwork
- [x] Verificar configuração do cloudflared — ver [11-cloudflare-tunnel.md](11-cloudflare-tunnel.md) seção 2
  - `kubectl get configmap -n cloudflared`
  - Identificar se usa Dashboard ou ConfigMap

---

## Fase 1 — self-workflows (Repositório Externo)

- [x] Adicionar input `image_tag` ao `self-workflows/.github/workflows/deploy.yml` — ver [08-github-actions.md](08-github-actions.md) seção 1
- [x] Usar `inputs.image_tag` no step de build da imagem
- [x] Fazer commit e push no repositório `leoferolive/self-workflows`
- [x] Verificar que o workflow reutilizável está disponível: `gh workflow list --repo leoferolive/self-workflows`

---

## Fase 2 — Backend

- [x] Verificar se `SpaController.java` existe — ver [03-backend.md](03-backend.md) seção 1
  - `find backend/src -name "SpaController.java"`
  - Se não existir: criar `backend/src/main/java/br/com/leoferolive/nossalista/config/SpaController.java`
- [x] Atualizar `application-dev.yml` para usar env vars em vez de IP hardcoded — ver [03-backend.md](03-backend.md) seção 2
  - `grep -n "192.168" backend/src/main/resources/application-dev.yml` deve retornar vazio
- [x] Verificar `application-prod.yml` (sem valores hardcoded) — ver [03-backend.md](03-backend.md) seção 3
- [x] Verificar `CorsConfig` aceita `http://nossalista.home` e `https://nossalista.leoferolive.com.br` — ver [03-backend.md](03-backend.md) seção 4

---

## Fase 3 — Frontend

- [x] Verificar que frontend não tem URLs hardcoded de backend — ver [04-frontend.md](04-frontend.md) seção 2
  - `grep -r "localhost:8080\|192.168" frontend/src/` deve retornar vazio
- [x] Verificar que WebSocket usa path relativo — ver [04-frontend.md](04-frontend.md) seção 4
- [x] Verificar que `npm run build` gera `dist/` corretamente
  - `cd frontend && npm run build && ls dist/`

---

## Fase 4 — Dockerfile

- [x] Substituir `Dockerfile` raiz pelo multi-stage com Node 22 + Maven 25 + JRE — ver [02-dockerfile.md](02-dockerfile.md) seção 2
- [x] Testar build local: `docker build -t nossalista:local .`
- [x] Testar container local com variáveis de ambiente — ver [02-dockerfile.md](02-dockerfile.md) seção 4
  - `curl http://localhost:8080/actuator/health` → `{"status":"UP"}`
  - `curl http://localhost:8080/` → HTML do React SPA

---

## Fase 5 — Manifests Kubernetes

- [x] Criar `k8s/dev/namespace.yaml` — ver [05-kubernetes.md](05-kubernetes.md) seção 2
- [x] Criar `k8s/dev/deployment.yaml` — ver [05-kubernetes.md](05-kubernetes.md) seção 3
- [x] Criar `k8s/dev/service.yaml` — ver [05-kubernetes.md](05-kubernetes.md) seção 4
- [x] Criar `k8s/dev/ingress.yaml` (host: `nossalista.home`) — ver [05-kubernetes.md](05-kubernetes.md) seção 5
- [x] Criar `k8s/prod/namespace.yaml` — ver [05-kubernetes.md](05-kubernetes.md) seção 6
- [x] Criar `k8s/prod/deployment.yaml` — ver [05-kubernetes.md](05-kubernetes.md) seção 7
- [x] Criar `k8s/prod/service.yaml` — ver [05-kubernetes.md](05-kubernetes.md) seção 8
- [x] Criar `k8s/prod/ingress.yaml` (host: `nossalista.leoferolive.com.br`) — ver [05-kubernetes.md](05-kubernetes.md) seção 9
- [x] Remover ou arquivar `k8s/*.yaml` raiz (substituídos por dev/ e prod/)

---

## Fase 6 — GitHub Actions

- [x] Criar `.github/workflows/deploy-dev.yml` — ver [08-github-actions.md](08-github-actions.md) seção 2
- [x] Criar `.github/workflows/deploy-prod.yml` — ver [08-github-actions.md](08-github-actions.md) seção 3
- [x] Remover `.github/workflows/deploy.yml` (legado) — ver [08-github-actions.md](08-github-actions.md) seção 4

---

## Fase 7 — GitHub Secrets

- [x] Gerar `TAILSCALE_AUTHKEY` (Reusable + Ephemeral) no Tailscale Admin — ver [09-github-secrets.md](09-github-secrets.md) seção 2
- [x] Configurar `TAILSCALE_AUTHKEY` no GitHub → Settings → Secrets
- [x] Gerar `KUBECONFIG` com IP Tailscale do K3s — ver [09-github-secrets.md](09-github-secrets.md) seção 3
  - `sed 's/127.0.0.1/<tailscale-ip>/' /etc/rancher/k3s/k3s.yaml | base64 -w 0`
- [x] Configurar `KUBECONFIG` no GitHub → Settings → Secrets
- [x] Verificar: `gh secret list --repo leoferolive/nossalista`

---

## Fase 8 — PostgreSQL

- [x] Acessar o pod: `kubectl exec -it deployment/postgres -n database -- psql -U root -d root`
- [x] Criar database e usuário dev — ver [07-banco-de-dados.md](07-banco-de-dados.md) seção 2
  - `CREATE USER nossalista_dev WITH PASSWORD '<senha>';`
  - `CREATE DATABASE nossalista_dev OWNER nossalista_dev;`
  - `GRANT ALL ON SCHEMA public TO nossalista_dev;`
- [x] Criar database e usuário prod — ver [07-banco-de-dados.md](07-banco-de-dados.md) seção 2
  - `CREATE USER nossalista WITH PASSWORD '<senha>';`
  - `CREATE DATABASE nossalista OWNER nossalista;`
  - `GRANT ALL ON SCHEMA public TO nossalista;`
- [x] Testar conexão: `psql -h 192.168.3.63 -p 30001 -U nossalista_dev -d nossalista_dev`

---

## Fase 9 — K8s Namespaces e Secrets

- [x] `kubectl apply -f k8s/dev/namespace.yaml`
- [x] `kubectl apply -f k8s/prod/namespace.yaml`
- [x] Copiar `ghcr-secret` para `nossalista-dev` — ver [06-secrets.md](06-secrets.md) seção 1
- [x] Copiar `ghcr-secret` para `nossalista` — ver [06-secrets.md](06-secrets.md) seção 1
- [x] Gerar JWT secrets: `openssl rand -base64 48`
- [x] Criar `nossalista-secrets` em `nossalista-dev` — ver [06-secrets.md](06-secrets.md) seção 2.1
- [x] Criar `nossalista-secrets` em `nossalista` — ver [06-secrets.md](06-secrets.md) seção 2.2
- [x] Verificar: `kubectl get secrets -n nossalista-dev && kubectl get secrets -n nossalista`

---

## Fase 10 — DNS e Acesso

- [x] Verificar/expor Traefik na porta 80 para rede local — ver [10-dns.md](10-dns.md) seção 3
- [x] Configurar DNS `nossalista.home → 192.168.3.63` — ver [10-dns.md](10-dns.md) seção 2
- [x] Testar resolução: `nslookup nossalista.home` → `192.168.3.63`
- [x] Adicionar hostname `nossalista.leoferolive.com.br` ao Cloudflare Tunnel — ver [11-cloudflare-tunnel.md](11-cloudflare-tunnel.md) seção 3 ou 4

---

## Fase 11 — Google OAuth

- [x] Acessar Google Cloud Console → projeto NossaLista → Credentials
- [x] Adicionar redirect URIs — ver [12-google-oauth.md](12-google-oauth.md) seção 1.1
  - `http://localhost:5173/api/auth/google/callback`
  - `http://localhost:8080/api/auth/google/callback`
  - `https://nossalista.leoferolive.com.br/api/auth/google/callback`
- [x] Adicionar authorized JavaScript origins — ver [12-google-oauth.md](12-google-oauth.md) seção 1.2
  - `http://localhost:5173`
  - `http://localhost:8080`
  - `https://nossalista.leoferolive.com.br`

---

## Fase 12 — Deploy e Validação

### 12.1 Deploy Dev
- [x] Aplicar manifests: `kubectl apply -f k8s/dev/`
- [x] Aguardar pod: `kubectl get pods -n nossalista-dev -w`
- [x] Verificar logs: `kubectl logs -f deployment/nossalista-dev -n nossalista-dev`
- [x] Testar health: `curl http://nossalista.home/actuator/health` → `{"status":"UP"}`
- [x] Testar SPA: `curl http://nossalista.home/` → HTML do React
- [x] Testar rota React Router: `curl http://nossalista.home/listas` → mesmo HTML (não 404)
- [x] Fazer push para `main` e verificar deploy automático via GitHub Actions
  - `gh run watch --workflow=deploy-dev.yml`

### 12.2 Deploy Prod
- [x] Aplicar manifests: `kubectl apply -f k8s/prod/`
- [x] Verificar pod: `kubectl get pods -n nossalista -w`
- [x] Testar prod via workflow_dispatch manual:
  - GitHub → Actions → "Deploy Prod" → Run workflow → confirm: `deploy`
- [x] Testar: `curl https://nossalista.leoferolive.com.br/actuator/health`
- [ ] Testar login com Google OAuth em ambos os ambientes

### 12.3 Verificação Final

```bash
# Dev
curl http://nossalista.home/actuator/health
curl http://nossalista.home/actuator/health/readiness

# Prod
curl https://nossalista.leoferolive.com.br/actuator/health

# K8s status
kubectl get pods -n nossalista-dev
kubectl get pods -n nossalista
kubectl get ingress -A

# GitHub Actions
gh run list --workflow=deploy-dev.yml --limit 3
gh run list --workflow=deploy-prod.yml --limit 3
```
