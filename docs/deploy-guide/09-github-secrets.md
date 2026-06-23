# GitHub Secrets — Configuração

## 1. Secrets Necessários no Repositório

Acessar: **GitHub → leoferolive/nossalista → Settings → Secrets and variables → Actions → New repository secret**

| Secret | Descrição | Obrigatório |
|--------|-----------|-------------|
| `TAILSCALE_AUTHKEY` | Auth key do Tailscale para conectar ao homelab | Sim |
| `KUBECONFIG` | Kubeconfig do K3s em base64 | Sim |

> `GITHUB_TOKEN` é automático — não precisa ser configurado.

---

## 2. TAILSCALE_AUTHKEY

O job `deploy` (em `deploy-environment.yml`, rodando em runner GitHub-hosted) usa `tailscale/github-action@v2` para entrar na tailnet e alcançar o kube-apiserver do K3s antes de executar `kubectl`. Ver `docs/DECISIONS.md` D-017.

### 2.1 Gerar no Tailscale Admin

1. Acessar [tailscale.com/admin/settings/keys](https://tailscale.com/admin/settings/keys)
2. **Generate auth key**
3. Marcar: **Reusable** + **Ephemeral**
4. Copiar a key gerada

### 2.2 Adicionar ao GitHub

```
Nome: TAILSCALE_AUTHKEY
Valor: tskey-auth-xxxxx...
```

### 2.3 Verificar Tailscale no cluster K3s

```bash
# No servidor k3s (Raspberry Pi)
tailscale status

# Deve mostrar o nó como conectado
# Se não instalado:
curl -fsSL https://tailscale.com/install.sh | sh
sudo tailscale up
```

---

## 3. KUBECONFIG

O `kubectl` nos workflows precisa de acesso ao cluster. O kubeconfig é fornecido em base64.

### 3.1 Gerar no servidor K3s

```bash
# No servidor k3s (Raspberry Pi)
cat /etc/rancher/k3s/k3s.yaml | base64 -w 0
# Copiar o output completo (string longa em uma linha)
```

### 3.2 Verificar o servidor no kubeconfig

O arquivo `k3s.yaml` por padrão usa `server: https://127.0.0.1:6443`. Para que o GitHub Actions consiga acessar via Tailscale, o IP deve ser o IP do Tailscale do nó:

```bash
# Ver o IP Tailscale do nó k3s
tailscale ip -4

# Editar k3s.yaml antes de base64-encodar (se necessário)
# Substituir 127.0.0.1 pelo IP Tailscale (ex: 100.x.x.x)
sed 's/127.0.0.1/<tailscale-ip>/' /etc/rancher/k3s/k3s.yaml | base64 -w 0
```

### 3.3 Adicionar ao GitHub

```
Nome: KUBECONFIG
Valor: <base64 do k3s.yaml com IP Tailscale>
```

---

## 4. Variáveis de Ambiente (Actions Variables)

Diferente de secrets, estas podem ser visíveis e não são criptografadas. Nenhuma variável adicional é necessária além dos secrets acima.

---

## 5. Verificar Secrets Configurados

```bash
# Via GitHub CLI
gh secret list --repo leoferolive/nossalista
```

Deve listar:
```
TAILSCALE_AUTHKEY  Updated recently
KUBECONFIG         Updated recently
```

---

## 6. Testar Conectividade (Simulação Local)

```bash
# Testar que o kubeconfig funciona do seu PC via Tailscale
KUBECONFIG=/path/to/k3s.yaml kubectl get nodes
# Esperado: leo-ubuntu   Ready   ...
```
