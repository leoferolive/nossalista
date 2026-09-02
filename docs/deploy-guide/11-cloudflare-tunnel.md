# Cloudflare Tunnel — Ambiente Prod

## 1. Situação Atual

O `cloudflared` já está rodando no namespace `cloudflared` e roteia `demo-api.leoferolive.com.br`. Precisamos adicionar o hostname `nossalista.leoferolive.com.br` ao mesmo tunnel.

---

## 2. Verificar Configuração Atual do Cloudflared

```bash
# Ver o deployment do cloudflared
kubectl get deployment cloudflared -n cloudflared -o yaml

# Ver ConfigMaps (pode conter o arquivo de config do tunnel)
kubectl get configmap -n cloudflared
kubectl describe configmap -n cloudflared

# Ver como a config é montada
kubectl get deployment cloudflared -n cloudflared -o yaml | grep -A20 "volumes:"
kubectl get deployment cloudflared -n cloudflared -o yaml | grep -A20 "volumeMounts:"
```

### Identificar o método de configuração

| Método | Indicador | Ação |
|--------|-----------|------|
| ConfigMap com `config.yaml` | Volume do tipo ConfigMap montado | Editar o ConfigMap (seção 3) |
| Cloudflare Dashboard | Sem arquivo de config no pod | Configurar via Dashboard (seção 4) |
| Variáveis de ambiente | `TUNNEL_URL` ou similar | Adicionar novo hostname via Dashboard |

---

## 3. Configurar via Cloudflare Zero Trust Dashboard (Recomendado)

Esta é a forma mais simples para adicionar um novo hostname:

1. Acessar [dash.cloudflare.com](https://dash.cloudflare.com)
2. Selecionar o domínio `leoferolive.com.br`
3. **Zero Trust** → **Networks** → **Tunnels**
4. Localizar o tunnel do homelab → **Edit**
5. **Public Hostnames** → **Add a public hostname**

Preencher:
```
Subdomain:  nossalista
Domain:     leoferolive.com.br
Type:       HTTP
URL:        nossalista.nossalista.svc.cluster.local:80
```

6. **Save hostname**

### Verificar o nome exato do serviço K8s

```bash
# Antes de configurar o tunnel, confirmar o service name
kubectl get service -n nossalista
# O URL interno será: nossalista.nossalista.svc.cluster.local:80
# Formato: <service-name>.<namespace>.svc.cluster.local:<port>
```

---

## 4. Configurar via ConfigMap (Se Usar Arquivo de Config)

Se o cloudflared usa um arquivo `config.yaml` montado via ConfigMap:

```bash
# Ver a config atual
kubectl get configmap <configmap-name> -n cloudflared -o yaml
```

Editar o ConfigMap para adicionar a rota do NossaLista:

```yaml
# config.yaml dentro do ConfigMap
tunnel: <tunnel-id>
credentials-file: /etc/cloudflared/creds/credentials.json
ingress:
  # Rota existente
  - hostname: demo-api.leoferolive.com.br
    service: http://demo-api.demo-api.svc.cluster.local:80

  # Nova rota — NossaLista Prod
  - hostname: nossalista.leoferolive.com.br
    service: http://nossalista.nossalista.svc.cluster.local:80

  # Fallback obrigatório
  - service: http_status:404
```

Aplicar a mudança:

```bash
kubectl edit configmap <configmap-name> -n cloudflared

# Reiniciar o cloudflared para recarregar a config
kubectl rollout restart deployment/cloudflared -n cloudflared
kubectl rollout status deployment/cloudflared -n cloudflared
```

---

## 5. HTTPS e TLS

O Cloudflare Tunnel termina TLS automaticamente:

- Usuário → `https://nossalista.leoferolive.com.br` (HTTPS com certificado gerenciado pelo Cloudflare)
- Cloudflare → cluster → `http://nossalista.nossalista.svc.cluster.local:80` (HTTP interno)

**Nenhum certificado TLS é necessário no cluster para o ambiente prod.**

O Ingress do prod usa apenas HTTP interno (`traefik.ingress.kubernetes.io/router.entrypoints: web`).

### Regra obrigatoria: HTTP -> HTTPS

Antes de publicar backend/frontend, configurar no Cloudflare Dashboard (zona do dominio):

1. **Rules -> Redirect Rules -> Create rule** (ou **Always Use HTTPS**, se essa opcao ja cobrir a zona).
2. Condicao: hostname `nossalista.leoferolive.com.br` e scheme `http`.
3. Acao: redirecionar para o mesmo host/path/query em `https`, status **308 Permanent Redirect**.

A regra roda na borda, antes do Cloudflare Tunnel: HTTP nao pode atingir OAuth nem emitir `Set-Cookie`. Nao habilitar `server.forward-headers-strategy` no Spring para tentar reconstruir o scheme; isso conflita com a protecao contra spoof de IP documentada em D-010. HSTS fica fora desta mudanca ate auditoria dos demais subdominios.

---

## 6. Verificação

```bash
# A borda deve forcar HTTPS antes de o tunnel receber a requisicao
curl -sSI http://nossalista.leoferolive.com.br/api/auth/google
# Esperado: HTTP/1.1 308, Location: https://nossalista.leoferolive.com.br/api/auth/google
# Nao pode haver Set-Cookie

# Após configurar o tunnel, testar de fora da rede local
curl -I https://nossalista.leoferolive.com.br/
# Esperado: HTTP/2 200 (ou 302 para login)

curl https://nossalista.leoferolive.com.br/actuator/health
# Esperado: {"status":"UP"}

# Verificar que o inicio Google preserva HTTPS com Location relativo
curl -sSI https://nossalista.leoferolive.com.br/api/auth/google
# Esperado: 302 e Location: /oauth2/authorization/google

# Verificar headers do Cloudflare
curl -v https://nossalista.leoferolive.com.br/ 2>&1 | grep -i "cf-ray\|server"
# Esperado: server: cloudflare
```

---

## 7. Troubleshooting

```bash
# Ver logs do cloudflared
kubectl logs -f deployment/cloudflared -n cloudflared

# Erros comuns:
# "no ingress rules match" → hostname não configurado no config.yaml
# "connection refused" → service K8s não existe ou pod não está rodando
# "502 Bad Gateway" → pod existe mas não responde (verificar readiness probe)

# Verificar que o service prod existe
kubectl get service nossalista -n nossalista
```
