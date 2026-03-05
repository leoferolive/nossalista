# Variaveis de Ambiente

## Backend

As principais variaveis sao lidas em `backend/src/main/resources/application.yml` e perfis.

Obrigatorias em producao:
- `DATABASE_PASSWORD`
- `JWT_SECRET`
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`

Importantes (com default em alguns cenarios):
- `DATABASE_URL` (default: `jdbc:postgresql://localhost:5432/nossalista`)
- `DATABASE_USER` (default: `nossalista`)
- `FRONTEND_URL` (default: `http://localhost:5173`)

Referencia de exemplo:
- `backend/.env.example`

## Frontend

No estado atual, o frontend usa a configuracao padrao de consumo da API definida no codigo.
Se novos `VITE_*` forem introduzidos, registrar aqui como obrigatorios/opcionais.

## Seguranca

- Nunca commitar `.env` com secrets reais.
- Em PRs, mascarar valores sensiveis em logs e capturas.
