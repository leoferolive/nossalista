# DNS e Acesso à Rede Local

## 1. Investigação: Como o Traefik Está Exposto

**Ação obrigatória antes de configurar DNS.** Verificar como o Traefik recebe tráfego da rede local:

```bash
# Verificar o tipo do service do Traefik
kubectl get service traefik -n traefik-system

# Ver a configuração completa
kubectl get service traefik -n traefik-system -o yaml

# Ver o DaemonSet/Deployment do Traefik
kubectl get daemonset traefik -n traefik-system 2>/dev/null \
  || kubectl get deployment traefik -n traefik-system

# Ver se usa hostNetwork
kubectl describe daemonset traefik -n traefik-system | grep -i "host\|network\|port"

# Ver os endpoints do service
kubectl get endpoints -n traefik-system
```

### Cenários Possíveis

| Situação | Solução |
|----------|---------|
| Service é `NodePort` na porta 80 | DNS aponta para IP do nó — pronto |
| Service é `LoadBalancer` com IP externo | DNS aponta para o IP externo |
| Service é `ClusterIP` + `hostNetwork: true` no DaemonSet | DNS aponta para IP do nó — pronto |
| Service é `ClusterIP` sem exposição externa | Precisa expor (ver seção 3) |

---

## 2. DNS para nossalista.home (Ambiente Dev)

O domínio `nossalista.home` deve resolver para `192.168.3.63` (IP do Raspberry Pi na rede local).

### Opção A — Pi-hole (Recomendada se já existir no homelab)

```
Pi-hole Admin → Local DNS → DNS Records
Host: nossalista.home
IP:   192.168.3.63
```

Ou via SSH no Pi-hole:
```bash
echo "address=/nossalista.home/192.168.3.63" >> /etc/dnsmasq.d/02-custom.conf
pihole restartdns
```

### Opção B — Roteador

Na interface web do roteador (geralmente `192.168.3.1`), procurar por "DNS local" ou "Hosts locais":
```
nossalista.home → 192.168.3.63
```

### Opção C — /etc/hosts (Apenas para máquina de desenvolvimento)

```bash
echo "192.168.3.63 nossalista.home" | sudo tee -a /etc/hosts
```

> **Limitação:** Funciona apenas na máquina onde foi configurado. Não resolve para outros dispositivos da rede.

### Verificar resolução DNS

```bash
nslookup nossalista.home
# Esperado: 192.168.3.63

ping nossalista.home
# Esperado: resposta do 192.168.3.63
```

---

## 3. Expor Traefik na Porta 80 (Se Necessário)

Se o Traefik for `ClusterIP` sem exposição para a rede local, uma das opções abaixo:

### Opção A — NodePort (Mais Simples)

Adicionar NodePort ao service do Traefik:

```bash
kubectl patch service traefik -n traefik-system \
  --type='json' \
  -p='[{"op": "replace", "path": "/spec/type", "value": "NodePort"},
       {"op": "add", "path": "/spec/ports/0/nodePort", "value": 30080}]'
```

Neste caso, o DNS aponta para `192.168.3.63` e o tráfego chega na porta `30080`. Não ideal (precisa porta não-padrão na URL).

**Alternativa melhor:** Usar iptables para redirecionar 80 → 30080 no nó:

```bash
sudo iptables -t nat -A PREROUTING -p tcp --dport 80 -j REDIRECT --to-port 30080
```

### Opção B — hostNetwork no Traefik

Se o Traefik usa DaemonSet, configurar `hostNetwork: true` para que ele escute diretamente nas portas do host:

```bash
kubectl patch daemonset traefik -n traefik-system \
  --type='json' \
  -p='[{"op": "add", "path": "/spec/template/spec/hostNetwork", "value": true}]'
```

### Opção C — MetalLB (LoadBalancer)

Instalar MetalLB para fornecer IPs de LoadBalancer na rede local. Mais complexo mas é a solução "correta" para K3s on-prem.

```bash
kubectl apply -f https://raw.githubusercontent.com/metallb/metallb/v0.14.9/config/manifests/metallb-native.yaml
```

---

## 4. Testar Acesso após Configuração

```bash
# Testar resolução DNS
curl -v http://nossalista.home/
# Esperado: HTML do React SPA

# Testar API
curl http://nossalista.home/api/health
# Ou
curl http://nossalista.home/actuator/health

# Testar que rotas do React Router funcionam
curl http://nossalista.home/listas
# Esperado: HTML do index.html (não 404)
```

---

## 5. DNS para nossalista.leoferolive.com.br (Ambiente Prod)

Gerenciado pelo Cloudflare Tunnel — ver [11-cloudflare-tunnel.md](11-cloudflare-tunnel.md). Nenhuma configuração DNS manual necessária.

---

## 6. Acesso ao Dev via Tailscale (Celular)

Para acessar o ambiente `nossalista-dev` fora da rede local, usar o `tailscale serve` no nó k3s apontando para o NodePort HTTP do Traefik (`31212`).

### Pré-condições

- Tailscale ativo no nó (`tailscale status`)
- Celular conectado na mesma tailnet
- Ingress dev com host `leo-ubuntu.tail7485fb.ts.net`

> **Host Tailscale como ponto único:** o hostname `leo-ubuntu.tail7485fb.ts.net` é
> específico do nó/tailnet atual e fica declarado em um único lugar:
> `k8s/dev/ingress.yaml` (regra `host:`). O fluxo de deploy
> (`deploy-environment.yml`) aplica os manifestos com `kubectl apply` direto, sem
> templating/`envsubst`. Se o nó ou a tailnet mudar, edite o host nesse manifesto
> e atualize as URLs desta seção. Não há substituição automática em tempo de deploy.

### Configurar proxy HTTPS no Tailscale

```bash
# No nó k3s
sudo tailscale serve --https=8443 --bg http://127.0.0.1:31212
```

### Verificar configuração

```bash
sudo tailscale serve status
```

Esperado:

```text
https://leo-ubuntu.tail7485fb.ts.net:8443 (tailnet only)
|-- / proxy http://127.0.0.1:31212
```

### URL de acesso no celular

```text
https://leo-ubuntu.tail7485fb.ts.net:8443/
```

### Remover a publicação (se necessário)

```bash
sudo tailscale serve --https=8443 off
```
