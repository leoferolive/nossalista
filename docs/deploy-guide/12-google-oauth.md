# Google OAuth — Configuração

## 1. Adicionar Redirect URIs no Google Cloud Console

1. Acessar [console.cloud.google.com](https://console.cloud.google.com)
2. Selecionar o projeto **NossaLista**
3. **APIs & Services** → **Credentials**
4. Localizar o **OAuth 2.0 Client ID** da aplicação → **Editar (ícone de lápis)**

### 1.1 Authorized redirect URIs

Adicionar as URIs para ambos os ambientes:

```
http://localhost:5173/api/auth/google/callback
http://localhost:8080/api/auth/google/callback
https://nossalista.leoferolive.com.br/api/auth/google/callback
```

### 1.2 Authorized JavaScript origins

```
http://localhost:5173
http://localhost:8080
https://nossalista.leoferolive.com.br
```

> Observação: o Google não aceita `.home` como origem OAuth. Para ambiente local, usar `localhost`.

5. **Save**

---

## 2. Mesmas Credenciais para Dois Ambientes

O mesmo `GOOGLE_CLIENT_ID` e `GOOGLE_CLIENT_SECRET` funciona para ambos os ambientes. O Google valida apenas que o `redirect_uri` enviado na requisição OAuth está na lista de URIs autorizadas.

**Vantagem:** Um único par de credenciais para gerenciar nos secrets K8s.

---

## 3. Verificar Configuração no Backend

O Spring Boot deve usar as credenciais via env vars (configuradas no secret `nossalista-secrets`):

```yaml
# application.yml ou application-dev/prod.yml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            # Prod: fixo em HTTPS para evitar callback http:// atrás de proxy/tunnel
            redirect-uri: https://nossalista.leoferolive.com.br/api/auth/google/callback
            scope: email, profile
```

Verificar o arquivo de configuração:

```bash
grep -r "google\|oauth2\|client-id" backend/src/main/resources/
```

---

## 4. Fluxo OAuth com Frontend Embutido

Com o frontend embutido, o fluxo é:

```
1. Usuário clica "Login com Google" em localhost (dev) ou no domínio de produção
2. Frontend redireciona para: GET /api/auth/google
3. Spring Security redireciona para accounts.google.com
4. Google autentica e redireciona para:
   http://localhost:8080/api/auth/google/callback?code=... (dev)
   ou
   https://nossalista.leoferolive.com.br/api/auth/google/callback?code=... (prod)
5. Spring Boot troca o code do Google por um JWT (OAuth2SuccessHandler)
6. Q2.3: o backend NÃO coloca o JWT na URL. Ele emite um one-time code opaco
   (single-use, TTL 60s) e redireciona para FRONTEND_URL/auth/callback?code=<code>
7. O frontend (AuthCallback) troca o one-time code pelo JWT em
   POST /api/auth/oauth/exchange e persiste o token no localStorage
8. Usuários Google entram com email_verified=true (e-mail já verificado pelo provedor)
```

> **Por que one-time code (Q2.3):** colocar o JWT direto na URL
> (`?token=<JWT>`) vazaria o token em histórico do browser, logs de servidor e
> header `Referer`. O one-time code é trocado por POST e descartado após o
> primeiro uso, sem mudar a arquitetura `localStorage` do frontend (decisão Q2.9).

---

## 5. Teste do Fluxo OAuth

```bash
# 1. Verificar que o endpoint de início do OAuth responde
curl -I http://localhost:8080/api/auth/google
# Esperado: 302 Found → Location: accounts.google.com/...

# 2. Verificar que o callback está registrado corretamente
# (Acessar no browser — precisa de sessão Google)
# http://localhost:8080/api/auth/google

# 3. Se retornar "redirect_uri_mismatch", verificar:
# - URI exata cadastrada no Google Cloud Console
# - FRONTEND_URL configurado no secret K8s
# - Configuração do redirect-uri no application.yml
```

---

## 6. Troubleshooting OAuth

| Erro | Causa | Solução |
|------|-------|---------|
| `redirect_uri_mismatch` | URI não cadastrada no Google ou callback saiu como `http://` | Adicionar ao Google Cloud Console e fixar `redirect-uri` HTTPS em `application-prod.yml` |
| `invalid_client` | Client ID/Secret errado | Verificar secret K8s |
| `access_blocked` | App não verificada (modo teste) | Adicionar usuários de teste no Google Console |
| Loop de redirect | Redirect pós-auth apontando para rota protegida | Verificar `FRONTEND_URL` no secret |

### Modo de teste (se necessário)

Se a app Google está em modo de teste (OAuth consent screen → Testing), adicionar os emails dos usuários de teste:

```
APIs & Services → OAuth consent screen → Test users → Add users
```
