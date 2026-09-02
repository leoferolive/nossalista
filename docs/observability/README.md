# Observabilidade

O NossaLista tem **dois** dashboards no Grafana do cluster compartilhado:

- **`NossaLista — Servidor MCP`** (uid `nossalista-mcp`) — chamadas, taxa de erro e latência
  das 13 tools MCP.
- **`NossaLista — Aplicação`** (uid `nossalista`) — infra da aplicação (req/s, latência,
  erro 5xx, JVM, CPU, uptime) + métricas de produto (usuários, DAU/WAU, listas, itens).

Ambos dependem da mesma coleta via Prometheus (seção abaixo) para os painéis de infra; o
dashboard de aplicação também usa um datasource Postgres separado para as métricas de produto.

## Métricas do servidor MCP

O NossaLista instrumenta as 13 tools do servidor MCP (`POST /mcp`) via Micrometer, através
do wrapper comum `McpToolMetrics` (`backend/src/main/java/br/com/leoferolive/nossalista/mcp/interceptor/McpToolMetrics.java`).
Cada chamada de tool passa por `McpToolMetrics.record(toolName, () -> ...)`, que registra:

- **`mcp_tool_calls_total{tool, outcome}`** (counter) — `outcome` é `success`, `business_error`
  (validação, não encontrado, sem permissão sobre o recurso — qualquer coisa que o SDK MCP
  converte em resultado de tool `isError: true`) ou `denied` (autenticação/escopo de PAT/rate
  limit — a chamada não chegou a executar a operação).
- **`mcp_tool_duration_seconds{tool}`** (timer, com `publishPercentileHistogram()`) — duração
  da chamada, incluindo os buckets `_bucket` necessários para `histogram_quantile` no Grafana.

As métricas ficam expostas, junto com as demais métricas padrão do Spring Boot Actuator, em
`GET /actuator/prometheus` (endpoint público — ver `SecurityConfig`).

### Por que não via Spring AOP

Ao contrário do padrão comum de instrumentar tools/handlers via um `@Aspect` com
`@Around("@annotation(...)")`, `McpToolMetrics` (e o rate limiter de mutação,
`McpMutationRateLimiter`) são chamados explicitamente — uma linha por tool, envolvendo o corpo
original. Um `@Aspect` sobre `@McpTool` força o Spring a criar um proxy CGLIB das classes de
tool (`ListMcpTools`, `ListItemMcpTools`, `MemberMcpTools`, `ActivityMcpTools`), e o SDK
`spring-ai-starter-mcp-server-webmvc` não tolera isso bem: ele invoca o método via reflection
sobre a instância exata capturada no seu scanner de anotações, e a segunda passagem pelos
filtros de segurança no completamento assíncrono do transporte Streamable HTTP deixa de
autenticar a requisição. Ver `docs/DECISIONS.md` D-023 para o histórico completo.

## Coleta pelo Prometheus (`ServiceMonitor`)

Expor o endpoint não basta: o Prometheus do `kube-prometheus-stack` só raspa alvos declarados
via CRD `ServiceMonitor`/`PodMonitor` com o label `release: kps` (exigido pelo
`serviceMonitorSelector` do `Prometheus` no cluster). Isso é feito em
`k8s/prod/servicemonitor.yaml` — raspa `/actuator/prometheus` a cada 30s. Sem esse objeto os
dois dashboards ficam com "No data" nos painéis baseados em Prometheus mesmo com o endpoint
funcionando normalmente (foi exatamente o que aconteceu até D-033 em `docs/DECISIONS.md`).
Hoje só `prod` está coberto — `dev` não tem `ServiceMonitor` (e `k8s/dev/service.yaml` também
não tem `name:` na porta, pré-requisito se isso mudar).

Verificar se está sendo raspado: `kubectl get servicemonitor -n nossalista`, ou via Prometheus
(`kubectl port-forward -n monitoring svc/kps-prometheus 9090:9090` → `/targets`, ou query
`up{namespace="nossalista"}` deve retornar `1`).

## Alertas (`PrometheusRule`)

`k8s/prod/prometheusrule.yaml` define 5 alertas, roteados automaticamente pro Telegram
(roteamento genérico por `severity` em `homelab/helm/kps-values.yaml`, não precisa de config
por app):

| Alerta | Severity | Trigger |
| --- | --- | --- |
| `NossaListaPodNotReady` | critical | Pod não-ready por 5min |
| `NossaListaPodCrashLooping` | critical | `CrashLoopBackOff` |
| `NossaListaPodRestarting` | warning | > 2 restarts em 15min |
| `NossaListaPodImagePullFailing` | warning | `ImagePullBackOff`/`ErrImagePull` |
| `NossaListaHttp5xxRateHigh` | warning | Erro 5xx > 5% em 5min (mesmo threshold do painel "Taxa de erro 5xx") |
| `NossaListaMcpErrorRateHigh` | warning | Erro MCP (business_error+denied) > 20% em 5min (mesmo threshold do painel MCP) |

## Dashboard "NossaLista — Servidor MCP"

`grafana-mcp-dashboard.json` é a fonte de verdade do dashboard e é provisionado
automaticamente no Grafana do cluster via **ConfigMap + sidecar**, o mesmo mecanismo já usado
pelo `chat-api` (ver `chat-api/k8s/monitoring/values.yaml`, bloco `grafana.sidecar.dashboards`):
o Grafana instalado pelo `kube-prometheus-stack` roda um sidecar (`grafana-sc-dashboards`) que
varre ConfigMaps com o label `grafana_dashboard=1` em **qualquer namespace**
(`searchNamespace: ALL`) e carrega o JSON encontrado automaticamente, sem restart do Grafana.

Por isso os painéis referenciam o datasource Prometheus por **UID fixo** (`"uid": "prometheus"`)
em vez da variável de import `${DS_PROMETHEUS}` — esse é o UID com que o `kube-prometheus-stack`
provisiona o datasource Prometheus por padrão no cluster.

O artefato versionado é `k8s/monitoring/nossalista-mcp-dashboard-configmap.yaml`: um ConfigMap
gerado a partir deste JSON (o `data` do ConfigMap **não** deve ser editado à mão — regenerar com
o comando documentado no cabeçalho do próprio arquivo sempre que `grafana-mcp-dashboard.json`
mudar).

Aplicar/atualizar no cluster:

```bash
kubectl apply -f k8s/monitoring/nossalista-mcp-dashboard-configmap.yaml
```

O dashboard inclui uma variável `tool` (multi-seleção, com "All") para filtrar os painéis por
uma ou mais tools específicas.

### Painéis

| Painel | Query base |
| --- | --- |
| Chamadas por minuto, por tool | `sum by (tool) (rate(mcp_tool_calls_total[1m])) * 60` |
| Taxa de erro (business_error + denied) / total | `sum(rate(mcp_tool_calls_total{outcome=~"business_error\|denied"}[5m])) / sum(rate(mcp_tool_calls_total[5m]))` |
| Duração p95, por tool | `histogram_quantile(0.95, sum by (tool, le) (rate(mcp_tool_duration_seconds_bucket[5m])))` |
| Top tools por volume (última hora) | `topk(10, sum by (tool) (increase(mcp_tool_calls_total[1h])))` |

## Dashboard "NossaLista — Aplicação"

Segundo dashboard do Grafana (uid `nossalista`), separado do dashboard MCP acima — cobre a
aplicação como um todo, não só as tools MCP. Fonte de verdade: `grafana-dashboard.json`,
provisionado via `k8s/monitoring/nossalista-dashboard-configmap.yaml` pelo mesmo mecanismo
ConfigMap + sidecar descrito acima (regenerar com o comando documentado no cabeçalho desse
YAML sempre que o JSON mudar). Aplicar/atualizar no cluster:

```bash
kubectl apply -f k8s/monitoring/nossalista-dashboard-configmap.yaml
```

Tem dois grupos de painéis, com datasources diferentes:

- **Infra (painéis 1-6), datasource Prometheus** — depende do `ServiceMonitor` (ver seção
  acima): req/s por rota (`http_server_requests_seconds_count`), latência p95 por rota
  (`http_server_requests_seconds_bucket`), taxa de erro 5xx, memória JVM heap/nonheap
  (`jvm_memory_used_bytes`/`jvm_memory_max_bytes`), CPU do container
  (`container_cpu_usage_seconds_total`), uptime do processo (`process_uptime_seconds`).
- **Produto (painéis 7-14), datasource Postgres somente-leitura `nossalista-pg`** — SQL direto
  no banco de produção (`users`, `list_items`, `lists`, `list_types`, `activity_logs`), sem
  depender de nenhuma métrica aplicativa: usuários totais, novos 7d/30d, DAU/WAU, taxa de
  onboarding, listas por tipo, itens totais, taxa de conclusão de itens, ações por dia.
  Datasource provisionado em `homelab/helm/kps-values.yaml` (usuário `grafana_ro`, senha via
  secret `grafana-ds-nossalista` no namespace `monitoring`) — **não é código deste
  repositório**, é infra do cluster compartilhado; qualquer mudança de schema que afete essas
  queries SQL precisa ser replicada lá manualmente (não há teste automatizado cobrindo isso).
