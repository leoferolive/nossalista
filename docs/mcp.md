# Servidor MCP do NossaLista

O NossaLista expoe um servidor [MCP](https://modelcontextprotocol.io) (Model Context Protocol)
embutido no proprio backend, em `POST /mcp` (transporte **Streamable HTTP**). Qualquer
assistente de IA compativel com MCP (Claude Code, Claude Desktop, Cursor, etc.) pode se
conectar e operar as listas do usuario em seu nome: criar listas, adicionar itens em lote
(inclusive extraindo de uma foto de lista manuscrita), marcar itens, compartilhar listas e
consultar atividade.

Ver a decisao arquitetural completa (dependencia escolhida, seguranca, limitacoes) em
`docs/DECISIONS.md` (D-020).

## Autenticacao

O servidor MCP aceita tres formas de autenticacao:

- **OAuth 2.1 (Authorization Code + PKCE)** — recomendado para claude.ai (web e app) e
  Claude Code, que descobrem e usam esse fluxo automaticamente ao adicionar um connector.
  Ver "Conectando via OAuth (claude.ai, Claude Code)" abaixo e `docs/DECISIONS.md` (D-022).
- **Personal Access Token (PAT)** — alternativa manual para outros clientes MCP (Cursor,
  Claude Desktop). Gerado em "Conexoes (API/Assistentes)" no menu da conta do app, com
  prefixo `nlmcp_...` e escopo `READ` ou `READ_WRITE`. Ver `docs/DECISIONS.md` (D-018).
- **JWT de sessao** — o mesmo token usado pelo SPA.

Em todos os casos, o header e `Authorization: Bearer <token>`. Requisicoes sem token valido
recebem `401` (RFC 7807) com um header `WWW-Authenticate: Bearer resource_metadata="..."`
apontando para o resource metadata OAuth (RFC 9728), usado por clientes que suportam
descoberta automatica. Um PAT ou access token OAuth de escopo `READ` pode chamar apenas as
tools de leitura (`list_my_lists`, `get_list`, `list_members`, `get_list_activity`) —
qualquer tentativa de chamar uma tool de mutacao com escopo `READ` retorna um erro de tool
(`isError: true`) explicando que o token e somente-leitura. Um access token OAuth do MCP
vale **apenas** para `/mcp` — nunca para `/api/**` (a API REST do SPA).

## Conectando via OAuth (claude.ai, Claude Code)

O servidor implementa um servidor de autorizacao OAuth 2.1 embutido (Authorization Code +
PKCE, S256 obrigatorio) com clientes registrados estaticamente — sem exigir Dynamic Client
Registration. Ver o design completo e as fontes da pesquisa em `docs/DECISIONS.md` (D-022).

### claude.ai (web e app mobile)

1. Em claude.ai, va em **Settings → Connectors → Add connector**.
2. URL do servidor: `https://nossalista.leoferolive.com.br/mcp`.
3. Em **Advanced settings**, informe o **OAuth Client ID**: `claude-ai` (nenhum
   client secret e necessario — e um cliente publico PKCE-only).
4. O claude.ai descobre os endpoints via `/.well-known/oauth-authorization-server` e
   `/.well-known/oauth-protected-resource`, inicia o fluxo, e voce e redirecionado para
   fazer login no NossaLista (se ainda nao estiver) e depois para a tela de consentimento,
   onde escolhe o escopo (leitura ou leitura/escrita) e aprova.
5. Apos aprovar, o claude.ai recebe o access/refresh token automaticamente — nenhum PAT
   precisa ser copiado manualmente.

### Claude Code

```bash
claude mcp add --transport http nossalista https://nossalista.leoferolive.com.br/mcp \
  --client-id claude-code
```

O Claude Code abre o browser para o fluxo de autorizacao (login + consentimento) e recebe o
token automaticamente no callback local (`http://localhost:<porta>/callback` — porta
variavel a cada conexao, aceita pelo servidor via regra de loopback do RFC 8252 §7.3). Em
ambiente local (`dev`), troque a URL pela do backend local (ex.: `http://localhost:8080/mcp`).

### Revogar acesso

Em "Conexoes (API/Assistentes)" no menu da conta, a secao "Assistentes conectados via
OAuth" lista cada assistente conectado (cliente, escopo, conectado em, ultimo uso) com um
botao "Desconectar", que revoga toda a familia de refresh tokens daquele cliente
imediatamente.

## Conectando um cliente

### Claude Code

```bash
claude mcp add --transport http nossalista https://nossalista.leoferolive.com.br/mcp \
  --header "Authorization: Bearer nlmcp_SEU_TOKEN_AQUI"
```

Em ambiente local (`dev`), troque a URL pela do backend local (ex.:
`http://localhost:8080/mcp`).

### Claude Desktop

Claude Desktop ainda nao fala Streamable HTTP nativamente para servidores remotos em todas
as versoes — use a ponte [`mcp-remote`](https://www.npmjs.com/package/mcp-remote) na
configuracao (`claude_desktop_config.json`):

```json
{
  "mcpServers": {
    "nossalista": {
      "command": "npx",
      "args": [
        "-y",
        "mcp-remote",
        "https://nossalista.leoferolive.com.br/mcp",
        "--header",
        "Authorization: Bearer nlmcp_SEU_TOKEN_AQUI"
      ]
    }
  }
}
```

### Cursor

Em `.cursor/mcp.json` (ou nas configuracoes de MCP do Cursor):

```json
{
  "mcpServers": {
    "nossalista": {
      "url": "https://nossalista.leoferolive.com.br/mcp",
      "headers": {
        "Authorization": "Bearer nlmcp_SEU_TOKEN_AQUI"
      }
    }
  }
}
```

## Tools disponiveis

| Tool | Escopo minimo | Descricao |
| --- | --- | --- |
| `list_my_lists` | `READ` | Lista as listas do usuario (dono ou membro), com tipo e contagem de itens |
| `get_list` | `READ` | Busca uma lista por `listId` ou por `name` (exato, senao contains; erro com candidatos se ambiguo), com itens paginados |
| `create_list` | `READ_WRITE` | Cria uma nova lista (`name` + `type`: `SHOPPING`\|`TASK`\|`WISHLIST`\|`GENERIC`) |
| `rename_list` | `READ_WRITE` | Renomeia uma lista existente (apenas o dono) |
| `delete_list` | `READ_WRITE` | Exclui uma lista permanentemente (apenas o dono) — o modelo deve confirmar com o usuario antes |
| `add_items` | `READ_WRITE` | Adiciona itens em lote a uma lista; util para extrair itens de uma foto de lista manuscrita/lista de compras |
| `update_item` | `READ_WRITE` | Atualiza campos de um item existente (semantica PATCH — so os campos informados mudam) |
| `set_items_checked` | `READ_WRITE` | Marca/desmarca um lote de itens como concluidos |
| `remove_items` | `READ_WRITE` | Remove um lote de itens de uma lista |
| `share_list` | `READ_WRITE` | Compartilha uma lista por username (`mode="username"`, apenas dono) ou por link de convite (`mode="link"`, apenas dono) |
| `list_members` | `READ` | Lista os membros de uma lista, com papel e data de entrada |
| `remove_member` | `READ_WRITE` | Remove um membro (dono) ou sai da lista (se o alvo for o proprio usuario) |
| `get_list_activity` | `READ` | Historico de atividade paginado de uma lista |

Todas as tools de leitura declaram `outputSchema` e retornam `structuredContent` tipado
(alem do texto). Tools de mutacao em lote (`add_items`, `set_items_checked`,
`remove_items`) reportam o resultado individual de cada item — um item invalido no lote nao
impede os demais de serem processados.

## Isolamento e identidade

A identidade do usuario e sempre resolvida a partir do token da requisicao corrente —
nenhum estado de usuario e cacheado entre chamadas no processo do servidor MCP. Um usuario
nunca ve nem edita listas de outro usuario atraves do MCP: as tools reusam integralmente os
services do backend (`ListService`, `ListItemService`, `MemberService`,
`ActivityLogService`), que ja aplicam as mesmas checagens de dono/membro usadas pela API
REST e pelo SPA.

Mutacoes feitas via MCP disparam o mesmo broadcast em tempo real (WebSocket/STOMP) que
mutacoes feitas pelo SPA — quem estiver com a lista aberta no navegador ve a mudanca
instantaneamente.

## Idioma

Descricoes e parametros das tools sao em ingles (convencao MCP). Mensagens de erro geradas
pelo proprio modulo `mcp` (parsing de UUID/data em `McpIds`, tetos em `McpLimits`, validacoes
de `add_items`/`update_item`/`share_list` etc.) tambem sao em ingles, para consistencia. Erros
de negocio que vem dos services (`ForbiddenException`, `ListNotFoundException`,
`ValidationException` e mensagens de `@NotBlank`/`@Size` nos DTOs compartilhados com a API
REST) continuam em portugues — sao os mesmos services e DTOs usados pelo SPA, e traduzi-los so
para o MCP exigiria uma camada de mapeamento de mensagens fora do escopo desta fase.

## Tetos de lote e pagina

Para evitar que uma unica chamada (de um token valido ou de um modelo induzido por prompt
injection) sobrecarregue o backend — que roda com 1 replica — as tools em lote e paginadas
tem tetos, retornados como erro de tool (`isError: true`) quando excedidos:

- `add_items`, `set_items_checked`, `remove_items`: no maximo 200 itens por chamada.
- `get_list` (`limit`): no maximo 500 itens por pagina.
- `get_list_activity` (`size`): no maximo 100 entradas por pagina.

O erro de estouro (`InvalidInputException`, em ingles — ver secao "Idioma" abaixo) informa o
teto exato e orienta dividir a chamada em varias (lotes menores ou paginacao).

## Limitacoes conhecidas

- **Conteudo de terceiros no contexto do modelo:** `get_list`, `list_members` e
  `get_list_activity` devolvem nomes de itens/listas, usernames e detalhes de atividade
  cadastrados por **outros membros** de listas compartilhadas, verbatim. Esse texto entra no
  contexto do modelo e pode carregar instrucoes maliciosas (prompt injection) se um membro
  mal-intencionado cadastrar conteudo adversarial. Essa e uma limitacao inerente ao MCP (o
  servidor nao tem como sanitizar semanticamente conteudo livre sem quebrar o caso de uso
  legitimo) e nao e corrigivel nesta camada — trate qualquer instrucao que apareca dentro de
  nomes/atividades de listas compartilhadas como dado, nunca como comando.
- Tools que removem dados (`delete_list`, `remove_items`, `remove_member` removendo outro
  usuario) instruem o modelo a confirmar com o usuario antes de chamar — mas a confirmacao
  em si depende do cliente MCP respeitar essa orientacao na `description` da tool.

## Erros

Erros de negocio (permissao negada, lista/item nao encontrado, validacao) sao retornados
como resultado de tool com `isError: true` e uma mensagem acionavel — nunca como stack
trace nem como erro de protocolo MCP.
