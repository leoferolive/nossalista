# Matriz de Autenticacao de Endpoints

Esta matriz registra os endpoints consumidos pelo frontend atual e o contrato esperado de autenticacao.

## Endpoints publicos intencionais

| Metodo | Endpoint | Origem no frontend | Contrato |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | fluxo de cadastro | Publico |
| `POST` | `/api/auth/login` | [Login.tsx](/home/leoferolive/projetos/nossalista/frontend/src/pages/Login.tsx) | Publico |
| `GET` | `/api/auth/google` | [Login.tsx](/home/leoferolive/projetos/nossalista/frontend/src/pages/Login.tsx), [JoinListPage.tsx](/home/leoferolive/projetos/nossalista/frontend/src/pages/JoinListPage.tsx) | Publico |
| `GET` | `/api/health` | monitoramento | Publico |
| `GET` | `/api/lists/join/{inviteCode}` | [listsApi.ts](/home/leoferolive/projetos/nossalista/frontend/src/api/listsApi.ts) | Publico para preview do convite |
| `WS` | `/ws/**` | [websocket.ts](/home/leoferolive/projetos/nossalista/frontend/src/api/websocket.ts) | Handshake permissivo; autenticacao exigida no CONNECT STOMP |

## Endpoints autenticados

| Metodo | Endpoint | Origem no frontend | Contrato |
| --- | --- | --- | --- |
| `GET` | `/api/users/me` | [AuthContext.tsx](/home/leoferolive/projetos/nossalista/frontend/src/contexts/AuthContext.tsx), [usersApi.ts](/home/leoferolive/projetos/nossalista/frontend/src/api/usersApi.ts) | Requer JWT |
| `PATCH` | `/api/users/me` | [usersApi.ts](/home/leoferolive/projetos/nossalista/frontend/src/api/usersApi.ts) | Requer JWT |
| `GET` | `/api/users/search` | [listsApi.ts](/home/leoferolive/projetos/nossalista/frontend/src/api/listsApi.ts) | Requer JWT |
| `POST` | `/api/lists` | [listsApi.ts](/home/leoferolive/projetos/nossalista/frontend/src/api/listsApi.ts) | Requer JWT |
| `GET` | `/api/lists` | [listsApi.ts](/home/leoferolive/projetos/nossalista/frontend/src/api/listsApi.ts) | Requer JWT |
| `GET` | `/api/lists/{id}` | [listsApi.ts](/home/leoferolive/projetos/nossalista/frontend/src/api/listsApi.ts) | Requer JWT |
| `GET` | `/api/lists/{id}/state` | [listsApi.ts](/home/leoferolive/projetos/nossalista/frontend/src/api/listsApi.ts) | Requer JWT |
| `PATCH` | `/api/lists/{id}` | [listsApi.ts](/home/leoferolive/projetos/nossalista/frontend/src/api/listsApi.ts) | Requer JWT |
| `DELETE` | `/api/lists/{id}` | [listsApi.ts](/home/leoferolive/projetos/nossalista/frontend/src/api/listsApi.ts) | Requer JWT |
| `POST` | `/api/lists/{id}/invite-link` | [listsApi.ts](/home/leoferolive/projetos/nossalista/frontend/src/api/listsApi.ts) | Requer JWT |
| `POST` | `/api/lists/join/{inviteCode}` | [listsApi.ts](/home/leoferolive/projetos/nossalista/frontend/src/api/listsApi.ts) | Requer JWT |
| `POST` | `/api/lists/{id}/invite` | [listsApi.ts](/home/leoferolive/projetos/nossalista/frontend/src/api/listsApi.ts) | Requer JWT |
| `GET` | `/api/lists/{id}/members` | [listsApi.ts](/home/leoferolive/projetos/nossalista/frontend/src/api/listsApi.ts) | Requer JWT |
| `DELETE` | `/api/lists/{id}/members/{userId}` | [listsApi.ts](/home/leoferolive/projetos/nossalista/frontend/src/api/listsApi.ts) | Requer JWT |
| `POST` | `/api/lists/{id}/leave` | [listsApi.ts](/home/leoferolive/projetos/nossalista/frontend/src/api/listsApi.ts) | Requer JWT |
| `GET` | `/api/lists/{id}/activity` | [listsApi.ts](/home/leoferolive/projetos/nossalista/frontend/src/api/listsApi.ts) | Requer JWT |
| `POST` | `/api/lists/{listId}/items` | [itemsApi.ts](/home/leoferolive/projetos/nossalista/frontend/src/api/itemsApi.ts) | Requer JWT |
| `GET` | `/api/lists/{listId}/items` | [itemsApi.ts](/home/leoferolive/projetos/nossalista/frontend/src/api/itemsApi.ts) | Requer JWT |
| `PATCH` | `/api/lists/{listId}/items/{itemId}/check` | [itemsApi.ts](/home/leoferolive/projetos/nossalista/frontend/src/api/itemsApi.ts) | Requer JWT |
| `PATCH` | `/api/lists/{listId}/items/{itemId}` | [itemsApi.ts](/home/leoferolive/projetos/nossalista/frontend/src/api/itemsApi.ts) | Requer JWT |
| `DELETE` | `/api/lists/{listId}/items/{itemId}` | [itemsApi.ts](/home/leoferolive/projetos/nossalista/frontend/src/api/itemsApi.ts) | Requer JWT |

## Observacoes

- O endpoint de atividades agora existe no backend e atende o contrato paginado esperado pelo frontend.
