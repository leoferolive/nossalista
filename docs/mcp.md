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

O servidor MCP aceita as duas formas de autenticacao ja suportadas pelo resto da API:

- **Personal Access Token (PAT)** — recomendado para assistentes de IA. Gerado em
  "Conexoes (API/Assistentes)" no menu da conta do app, com prefixo `nlmcp_...` e escopo
  `READ` ou `READ_WRITE`. Ver `docs/DECISIONS.md` (D-018).
- **JWT de sessao** — o mesmo token usado pelo SPA.

Em ambos os casos, o header e `Authorization: Bearer <token>`. Requisicoes sem token valido
recebem `401` (RFC 7807). Um PAT de escopo `READ` pode chamar apenas as tools de leitura
(`list_my_lists`, `get_list`, `list_members`, `get_list_activity`) — qualquer tentativa de
chamar uma tool de mutacao com um PAT `READ` retorna um erro de tool (`isError: true`)
explicando que o token e somente-leitura.

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

## Tetos de lote e pagina

Para evitar que uma unica chamada (de um token valido ou de um modelo induzido por prompt
injection) sobrecarregue o backend — que roda com 1 replica — as tools em lote e paginadas
tem tetos, retornados como erro de tool (`isError: true`) quando excedidos:

- `add_items`, `set_items_checked`, `remove_items`: no maximo 200 itens por chamada.
- `get_list` (`limit`) e `get_list_activity` (`size`): no maximo 500 por pagina.

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
