## ADDED Requirements

### Requirement: Dependências Micrometer e OpenTelemetry no pom.xml
O `pom.xml` SHALL incluir as extensões `quarkus-micrometer`, `quarkus-opentelemetry` e o bridge `micrometer-registry-otlp` (ou equivalente do BOM do Quarkus 3.26.x), permitindo que métricas Micrometer sejam exportadas via OTLP.

#### Scenario: Build com sucesso após adição das dependências
- **WHEN** o projeto é compilado com `mvn package`
- **THEN** o build conclui sem erros de conflito de versão

#### Scenario: Endpoint de métricas disponível em desenvolvimento
- **WHEN** a aplicação sobe localmente e é feita uma requisição para `/q/metrics`
- **THEN** a resposta retorna métricas no formato Prometheus com status 200

---

### Requirement: Configuração OTLP como exporter de métricas
A aplicação SHALL ser configurada para exportar métricas Micrometer via protocolo OTLP para `localhost:4317` (gRPC), com intervalo de push de 60 segundos.

#### Scenario: Exportação OTLP configurada corretamente
- **WHEN** a aplicação inicia com `QUARKUS_OTEL_EXPORTER_OTLP_METRICS_ENDPOINT=http://localhost:4317`
- **THEN** logs de startup mostram o exporter OTLP inicializado sem erros

#### Scenario: Falha silenciosa quando collector não disponível
- **WHEN** a aplicação sobe e o ADOT Collector não está disponível na porta 4317
- **THEN** a aplicação inicia normalmente e serve requests sem interrupção (erro de exportação não é fatal)

---

### Requirement: Métricas HTTP automáticas para Latência e Tráfego
A extensão `quarkus-micrometer` SHALL instrumentar automaticamente todos os endpoints REST, expondo:
- `http.server.requests` (counter + histogram) com tags `uri`, `method`, `status`, `outcome`

#### Scenario: Métrica de latência registrada após request
- **WHEN** uma requisição HTTP é feita para qualquer endpoint da aplicação
- **THEN** a métrica `http_server_requests_seconds` é incrementada com as tags corretas de método, URI e status HTTP

#### Scenario: Métricas de erro registradas para respostas 4xx/5xx
- **WHEN** um endpoint retorna status 400, 401, 403, 404, 500 ou 503
- **THEN** a tag `outcome` da métrica é `CLIENT_ERROR` ou `SERVER_ERROR` respectivamente

---

### Requirement: Métricas de Saturação via JVM e Connection Pool
A aplicação SHALL expor automaticamente métricas de saturação via Micrometer:
- `jvm.memory.used` / `jvm.memory.max` por heap region
- `agroal.connections.active` (DB connection pool — via extensão Quarkus JDBC)

#### Scenario: Métricas JVM presentes no endpoint de métricas
- **WHEN** a aplicação está rodando e `/q/metrics` é consultado
- **THEN** métricas com prefixo `jvm_` estão presentes no output

#### Scenario: Métricas de pool de conexão DB presentes
- **WHEN** a aplicação está rodando com datasource PostgreSQL configurado
- **THEN** métricas com prefixo `agroal_` estão presentes indicando conexões ativas e disponíveis
