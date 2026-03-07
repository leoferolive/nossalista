# Deploy Guide — NossaLista (Dois Ambientes)

Documentação completa para configurar e operar os ambientes de deploy da aplicação NossaLista.

## Visão Geral

A NossaLista é implantada como **um único container** (frontend React embutido no JAR do Spring Boot), rodando em um cluster K3s em Raspberry Pi 4 com dois ambientes distintos:

- **Dev** (`nossalista.home`): atualizado automaticamente a cada push em `release/*`
- **Prod** (`nossalista.leoferolive.com.br`): release automática em push na `main`, com aprovação obrigatória de environment antes do deploy

## Tabela de Ambientes

| Atributo            | Dev                                    | Prod                                          |
|---------------------|----------------------------------------|-----------------------------------------------|
| Domínio             | `nossalista.home`                      | `nossalista.leoferolive.com.br`               |
| Namespace K8s       | `nossalista-dev`                       | `nossalista`                                  |
| Image tag           | `ghcr.io/leoferolive/nossalista:dev`   | `ghcr.io/leoferolive/nossalista:vX.Y.Z`       |
| Trigger deploy      | Push em `release/*`                    | Push na `main` + aprovação no environment `production` |
| Spring profile      | `dev`                                  | `prod`                                        |
| Banco de dados      | `nossalista_dev`                       | `nossalista`                                  |
| Acesso              | Rede local (Traefik direto)            | Internet (Cloudflare Tunnel)                  |
| HTTPS               | Não (HTTP)                             | Sim (via Cloudflare)                          |

## Documentos

| Arquivo | Conteúdo |
|---------|----------|
| [01-arquitetura.md](01-arquitetura.md) | Estratégia de imagem única, fluxo de deploy, diagrama |
| [02-dockerfile.md](02-dockerfile.md) | Dockerfile multi-stage (Node + Maven + JRE) |
| [03-backend.md](03-backend.md) | SpaController, application-dev.yml, CORS |
| [04-frontend.md](04-frontend.md) | Vite build, variáveis de ambiente |
| [05-kubernetes.md](05-kubernetes.md) | Todos os manifests K8s (dev + prod) |
| [06-secrets.md](06-secrets.md) | Secrets K8s: ghcr-secret e nossalista-secrets |
| [07-banco-de-dados.md](07-banco-de-dados.md) | Setup PostgreSQL: databases e usuários |
| [08-github-actions.md](08-github-actions.md) | Workflows CI/CD (deploy-dev + release-prod) |
| [09-github-secrets.md](09-github-secrets.md) | Secrets necessários no repositório GitHub |
| [10-dns.md](10-dns.md) | DNS para nossalista.home + exposição Traefik |
| [11-cloudflare-tunnel.md](11-cloudflare-tunnel.md) | Cloudflare Tunnel para domínio prod |
| [12-google-oauth.md](12-google-oauth.md) | Redirect URIs no Google Cloud Console |
| [TASKS.md](TASKS.md) | **Lista mestre de execução** com checklist por fase |

## Pré-requisitos

### Acesso ao Cluster
- `kubectl` configurado com acesso ao cluster K3s
- Tailscale instalado e conectado ao nó K3s (`tailscale status`)
- Permissão de `kubectl apply` nos namespaces `nossalista-dev` e `nossalista`

### Credenciais Necessárias
- Acesso ao Google Cloud Console (projeto NossaLista) para configurar OAuth
- Acesso ao Cloudflare Zero Trust Dashboard para configurar tunnel
- Acesso ao GitHub → Settings → Secrets para configurar `TAILSCALE_AUTHKEY` e `KUBECONFIG`
- Senha do PostgreSQL (usuário `root`, namespace `database`)

### Ferramentas
- `docker` (para testar build local)
- `kubectl`
- `gh` (GitHub CLI, opcional)

## Ordem de Execução

Consulte [TASKS.md](TASKS.md) para a lista completa organizada por fase. A ordem recomendada é:

1. Preparação (acesso, investigação Traefik)
2. self-workflows (adicionar `image_tag`)
3. Backend + Frontend (código)
4. Dockerfile (build multi-stage)
5. Manifests K8s
6. GitHub Actions
7. GitHub Secrets
8. PostgreSQL (databases)
9. K8s Namespaces e Secrets
10. DNS e Acesso
11. Google OAuth
12. Deploy e Validação
