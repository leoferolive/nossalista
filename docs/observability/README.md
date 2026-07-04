# Observabilidade do servidor MCP

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

## Por que não via Spring AOP

Ao contrário do padrão comum de instrumentar tools/handlers via um `@Aspect` com
`@Around("@annotation(...)")`, `McpToolMetrics` (e o rate limiter de mutação,
`McpMutationRateLimiter`) são chamados explicitamente — uma linha por tool, envolvendo o corpo
original. Um `@Aspect` sobre `@McpTool` força o Spring a criar um proxy CGLIB das classes de
tool (`ListMcpTools`, `ListItemMcpTools`, `MemberMcpTools`, `ActivityMcpTools`), e o SDK
`spring-ai-starter-mcp-server-webmvc` não tolera isso bem: ele invoca o método via reflection
sobre a instância exata capturada no seu scanner de anotações, e a segunda passagem pelos
filtros de segurança no completamento assíncrono do transporte Streamable HTTP deixa de
autenticar a requisição. Ver `docs/DECISIONS.md` D-023 para o histórico completo.

## Provisionamento do dashboard no cluster

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

