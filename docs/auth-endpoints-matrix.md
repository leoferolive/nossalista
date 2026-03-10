# Matriz de Autenticacao de Endpoints

Esta matriz registra os endpoints consumidos pelo frontend atual e o contrato esperado de autenticacao.

## Endpoints publicos intencionais

| Metodo | Endpoint | Origem no frontend | Contrato |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | fluxo de cadastro | Publico |
| `POST` | `/api/auth/login` | `frontend/src/pages/Login.tsx` | Publico |
| `GET` | `/api/auth/google` | `frontend/src/pages/Login.tsx`, `frontend/src/pages/JoinListPage.tsx` | Publico |
| `GET` | `/api/health` | monitoramento e auditoria operacional | Publico; retorna status, version, gitSha, gitTag, environment e buildTime |
| `GET` | `/api/lists/join/{inviteCode}` | `frontend/src/api/listsApi.ts` | Publico para preview do convite |
| `WS` | `/ws/**` | `frontend/src/api/websocket.ts` | Handshake permissivo; autenticacao exigida no CONNECT STOMP |

## Endpoints autenticados

| Metodo | Endpoint | Origem no frontend | Contrato |
| --- | --- | --- | --- |
| `GET` | `/api/users/me` | `frontend/src/contexts/AuthContext.tsx`, `frontend/src/api/usersApi.ts` | Requer JWT |
| `PATCH` | `/api/users/me` | `frontend/src/api/usersApi.ts` | Requer JWT |
| `POST` | `/api/users/me/onboarding/complete` | `frontend/src/contexts/OnboardingContext.tsx` | Requer JWT |
| `GET` | `/api/users/search` | `frontend/src/api/listsApi.ts` | Requer JWT |
| `POST` | `/api/lists` | `frontend/src/api/listsApi.ts` | Requer JWT |
| `GET` | `/api/lists` | `frontend/src/api/listsApi.ts` | Requer JWT |
| `GET` | `/api/lists/{id}` | `frontend/src/api/listsApi.ts` | Requer JWT |
| `GET` | `/api/lists/{id}/state` | `frontend/src/api/listsApi.ts` | Requer JWT |
| `PATCH` | `/api/lists/{id}` | `frontend/src/api/listsApi.ts` | Requer JWT |
| `DELETE` | `/api/lists/{id}` | `frontend/src/api/listsApi.ts` | Requer JWT |
| `POST` | `/api/lists/{id}/invite-link` | `frontend/src/api/listsApi.ts` | Requer JWT |
| `POST` | `/api/lists/join/{inviteCode}` | `frontend/src/api/listsApi.ts` | Requer JWT |
| `POST` | `/api/lists/{id}/invite` | `frontend/src/api/listsApi.ts` | Requer JWT |
| `GET` | `/api/lists/{id}/members` | `frontend/src/api/listsApi.ts` | Requer JWT |
| `DELETE` | `/api/lists/{id}/members/{userId}` | `frontend/src/api/listsApi.ts` | Requer JWT |
| `POST` | `/api/lists/{id}/leave` | `frontend/src/api/listsApi.ts` | Requer JWT |
| `GET` | `/api/lists/{id}/activity` | `frontend/src/api/listsApi.ts` | Requer JWT |
| `POST` | `/api/lists/{listId}/items` | `frontend/src/api/itemsApi.ts` | Requer JWT |
| `GET` | `/api/lists/{listId}/items` | `frontend/src/api/itemsApi.ts` | Requer JWT |
| `PATCH` | `/api/lists/{listId}/items/{itemId}/check` | `frontend/src/api/itemsApi.ts` | Requer JWT |
| `PATCH` | `/api/lists/{listId}/items/{itemId}` | `frontend/src/api/itemsApi.ts` | Requer JWT |
| `DELETE` | `/api/lists/{listId}/items/{itemId}` | `frontend/src/api/itemsApi.ts` | Requer JWT |

## Observacoes

- O endpoint de atividades agora existe no backend e atende o contrato paginado esperado pelo frontend.
- `POST /api/auth/login` e `GET /api/users/me` retornam `onboardingCompletedAt` para controlar o tutorial de primeiro login por conta.
