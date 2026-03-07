# Banco de Dados — Setup PostgreSQL

## 1. Infraestrutura Existente

O PostgreSQL já está rodando no cluster:

| Atributo | Valor |
|----------|-------|
| Namespace | `database` |
| Imagem | `postgres:17` |
| NodePort | `30001` |
| Usuário root | `root` |
| DNS interno | `postgres.database.svc.cluster.local:5432` |
| Acesso externo | `192.168.3.63:30001` |

---

## 2. Criar Databases e Usuários

### 2.1 Acessar o Pod do PostgreSQL

```bash
kubectl exec -it deployment/postgres -n database -- psql -U root -d root
```

### 2.2 SQL de Setup

```sql
-- ============================================
-- Ambiente Dev
-- ============================================
CREATE USER nossalista_dev WITH PASSWORD '<senha-dev>';
CREATE DATABASE nossalista_dev OWNER nossalista_dev;
GRANT ALL PRIVILEGES ON DATABASE nossalista_dev TO nossalista_dev;

-- Permissões adicionais (Flyway precisa criar tabelas no schema public)
\c nossalista_dev
GRANT ALL ON SCHEMA public TO nossalista_dev;

-- ============================================
-- Ambiente Prod
-- ============================================
\c root
CREATE USER nossalista WITH PASSWORD '<senha-prod>';
CREATE DATABASE nossalista OWNER nossalista;
GRANT ALL PRIVILEGES ON DATABASE nossalista TO nossalista;

-- Permissões adicionais
\c nossalista
GRANT ALL ON SCHEMA public TO nossalista;

\q
```

### 2.3 Verificar criação

```sql
-- Listar databases
\l

-- Listar usuários
\du

-- Testar conexão com usuário dev
\c nossalista_dev nossalista_dev
\q
```

---

## 3. Flyway — Migrations

O Spring Boot executa as migrations automaticamente ao iniciar, usando as credenciais configuradas no secret `nossalista-secrets`.

**Verificar que o usuário tem permissões suficientes:**

```sql
-- Conectar como nossalista_dev e verificar permissões
\c nossalista_dev nossalista_dev
\dn+
-- schema public deve ter ALL para nossalista_dev
```

Se as migrations falharem por permissão, executar como `root`:

```sql
\c nossalista_dev root
ALTER SCHEMA public OWNER TO nossalista_dev;
```

---

## 4. Acesso ao Banco de Dados

### 4.1 Dentro do Cluster (DNS interno)

```
jdbc:postgresql://postgres.database.svc.cluster.local:5432/nossalista_dev
jdbc:postgresql://postgres.database.svc.cluster.local:5432/nossalista
```

### 4.2 Acesso Externo (Dev Local / Diagnóstico)

```bash
# Via NodePort (requer acesso à rede local 192.168.3.63)
psql -h 192.168.3.63 -p 30001 -U nossalista_dev -d nossalista_dev

# Via kubectl port-forward
kubectl port-forward service/postgres 5432:5432 -n database
psql -h localhost -p 5432 -U nossalista_dev -d nossalista_dev
```

---

## 5. Testar Conexão do Pod da Aplicação

Após o deploy, verificar que o pod consegue conectar ao banco:

```bash
# Ver logs de inicialização (procurar por Flyway ou HikariCP)
kubectl logs -f deployment/nossalista-dev -n nossalista-dev | grep -E "Flyway|HikariCP|Started|ERROR"

# Health check (readinessProbe verifica conexão ao banco)
kubectl exec -it deployment/nossalista-dev -n nossalista-dev -- \
  curl -s http://localhost:8080/actuator/health | python3 -m json.tool
# "db": {"status": "UP"} indica conexão ok
```

---

## 6. Backup (Opcional)

```bash
# Dump do banco prod antes de migrations
kubectl exec -it deployment/postgres -n database -- \
  pg_dump -U nossalista nossalista > nossalista_backup_$(date +%Y%m%d).sql

# Restore
kubectl exec -i deployment/postgres -n database -- \
  psql -U nossalista nossalista < nossalista_backup_20250101.sql
```
