# Embed do Langflow: contrato do token

Spike descartável. O NossaLista emite um JWT RS256 curto para o usuário já logado e publica a chave pública para o Langflow validar esse token. Desligado por padrão (`app.langflow-embed.enabled=false`).

Este documento é o contrato para quem configura o Langflow. Como o Langflow lê o cookie e o JWKS (External Auth) não está neste repositório: **a confirmar** na configuração do lado Langflow.

## Fluxo

1. O usuário está logado no NossaLista (cookie de sessão `__Host-nl_session` por padrão; `nl_session` só em dev e test).
2. `GET /api/langflow/launch` (exige sessão web: anônimo, token OAuth do MCP e qualquer Bearer que não seja PAT recebem 401; PAT recebe 403):
   - grava o cookie `nl_lf_token` com o JWT;
   - responde `302 Location: /langflow/`.
3. O Langflow recebe o cookie em requisições sob `/langflow` e valida o JWT com a chave do JWKS.
4. Ao expirar (15 min), o usuário repete o launch.

## Cookie

| Atributo | Valor |
| --- | --- |
| Nome | `nl_lf_token` (`app.langflow-embed.cookie-name`) |
| Valor | JWT compacto RS256 |
| `Path` | `/langflow` |
| `Max-Age` | igual ao `token-ttl` (900 s por padrão) |
| `HttpOnly` | sim |
| `SameSite` | `Lax` |
| `Secure` | sim por padrão (`LANGFLOW_EMBED_COOKIE_SECURE=false` só em dev HTTP) |
| `Domain` | ausente: o cookie fica restrito ao host que emitiu |

O cookie não é credencial de sessão do NossaLista: `/api/**` não o aceita (ele não é `__Host-nl_session`, e o token não passa na checagem de sessão).

No logout, com o embed ligado, o NossaLista também grava `nl_lf_token` com `Max-Age=0` e os mesmos atributos do launch. Com o embed desligado, nenhum cookie do Langflow é escrito.

## JWKS

`GET /.well-known/jwks.json` é público e responde assim:

```json
{
  "keys": [
    {
      "kty": "RSA",
      "use": "sig",
      "alg": "RS256",
      "kid": "nl-langflow-1",
      "n": "<módulo, base64url sem padding>",
      "e": "<expoente público, base64url sem padding>"
    }
  ]
}
```

Hoje há uma única chave, com `kid` vindo de `app.langflow-embed.key-id` (default `nl-langflow-1`). Não há rotação com duas chaves publicadas ainda.

## Token

Header: `{"alg":"RS256","kid":"nl-langflow-1"}`.

| Claim | Valor |
| --- | --- |
| `iss` | `LANGFLOW_EMBED_ISSUER`, exatamente como configurado |
| `aud` | string `"langflow"` (`app.langflow-embed.audience`) |
| `sub` | UUID do usuário no NossaLista |
| `email` | e-mail do usuário |
| `preferred_username` | username do usuário |
| `name` | nome de exibição; se vazio, o username |
| `iat` | emissão (segundos desde a época) |
| `exp` | `iat` + 15 min |

Não há claim de papel ou de lista. Autorização por lista e por usuário fica do lado do Langflow, se for necessária.

## Como validar

Qualquer validador RS256 com JWKS serve. Exemplo de checagem da chave publicada:

```bash
curl -s https://<host-do-nossalista>/.well-known/jwks.json | jq '.keys[0] | {kid, alg, use, kty}'
```

Validar o token exige checar assinatura por `kid`, `iss` igual ao issuer, `aud` igual a `langflow` e `exp`.

## Limitações do spike

- **Sem revogação.** O token é stateless. O logout expira o cookie `nl_lf_token` no navegador, mas quem já tiver uma cópia do JWT a mantém válida até o `exp`, ou seja, até 15 min. Não há lista de revogação.
- **Sem rotação de chave.** Trocar a chave exige republicar o JWKS com uma chave só, o que invalida os tokens emitidos antes.
- **Chave em variável de ambiente.** `LANGFLOW_EMBED_RSA_PRIVATE_KEY` é segredo: nunca em arquivo versionado nem em `application*.yml`.

## Configuração

Variáveis e defaults em `docs/ENVIRONMENT.md`, seção "Embed do Langflow". Cobertura dos testes: `backend/src/test/java/br/com/leoferolive/nossalista/langflowembed/`.
