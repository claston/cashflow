# Especificação para GPT 5.4 — POC Local de Arquitetura de Fluxo de Caixa

## 1. Papel do agente

Você é um engenheiro Java/Spring Boot sênior. Sua tarefa é implementar o arcabouço principal de uma POC local de arquitetura de fluxo de caixa usando monólito modular Spring Boot.

O objetivo é provar a viabilidade arquitetural, não criar uma solução de produção completa.

A aplicação deve rodar localmente em uma máquina pessoal fraca, sem Docker, sem AWS, sem Kafka, sem Redis e sem DynamoDB.

---

## 2. Objetivo da POC

Criar uma aplicação que demonstre:

- escrita transacional de lançamentos financeiros;
- separação entre comando e consulta;
- Outbox Pattern;
- publicação assíncrona de eventos;
- fila em memória;
- worker de projeção;
- read model materializado em memória;
- consistência eventual;
- fallback de consulta;
- rate limit local;
- resiliência mínima;
- endpoints de diagnóstico;
- logs claros para demonstrar o fluxo.

A arquitetura deve ser local-first, mas preparada para evolução futura para infraestrutura real.

---

## 3. Stack obrigatória

Use:

- Java 21, ou Java 17 se necessário;
- Spring Boot;
- Spring Web;
- Spring Data JPA;
- H2 Database em modo arquivo;
- Bean Validation;
- Spring Scheduler;
- Spring Actuator;
- JUnit.

Opcional:

- Swagger/OpenAPI.

Não usar:

- Docker;
- AWS;
- Kafka;
- SQS;
- DynamoDB;
- Redis;
- Debezium;
- Kubernetes;
- frontend.

---

## 4. Decisão arquitetural

Implementar como monólito modular Spring Boot.

A aplicação roda em um único processo, mas deve ter separação clara entre módulos internos.

Arquitetura conceitual:

```text
[Cliente HTTP / Postman / Swagger]
        |
        v
[Spring Boot App]
        |
        +------------------------------+
        |                              |
        v                              v
[Command API]                    [Query API]
        |                              |
        v                              v
[CashFlowEntry Table]       [InMemory Read Model]
        |
        v
[OutboxEvent Table]
        |
        v
[OutboxPublisher @Scheduled]
        |
        v
[InMemory Event Bus]
        |
        v
[ProjectionWorker @Scheduled]
        |
        v
[InMemory Read Model]
```

A separação física em múltiplos serviços é uma evolução futura, não parte da POC.

---

## 5. Estrutura de pacotes esperada

Use uma estrutura próxima desta:

```text
src/main/java/com/example/cashflow

  CashflowApplication.java

  shared
    clock
    exception
    json
    validation

  cashflow
    command
      api
      application
      domain
      infrastructure

    query
      api
      application
      domain
      infrastructure

    projection
      application
      domain
      infrastructure

  outbox
    api
    application
    domain
    infrastructure

  eventbus
    domain
    infrastructure

  ratelimit
    api
    application
    infrastructure

  diagnostics
    api
    application
```

Regra importante:

- Controller não deve conter regra de negócio.
- Use cases não devem depender diretamente de implementações concretas quando existir interface.
- Infraestrutura deve implementar contratos definidos nos módulos de aplicação/domínio.
- O domínio não deve conhecer Spring, JPA ou detalhes de infraestrutura.

---

## 6. Configuração local

Usar H2 em modo arquivo.

`application.properties` sugerido:

```properties
spring.datasource.url=jdbc:h2:file:./data/cashflow;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

spring.task.scheduling.pool.size=4

management.endpoints.web.exposure.include=health,info

app.outbox.publisher-delay-ms=2000
app.outbox.batch-size=10
app.outbox.max-attempts=3

app.projection.worker-delay-ms=1000
app.projection.max-events-per-cycle=20

app.rate-limit.enabled=true
app.rate-limit.requests-per-minute=60
```

---

## 7. Modelo de domínio

### 7.1 CashFlowEntry

Representa um lançamento financeiro.

Campos:

```text
id UUID
companyId UUID
description String
type CashFlowEntryType
amount BigDecimal
category String
account String
dueDate LocalDate
paymentDate LocalDate nullable
status CashFlowEntryStatus
createdAt Instant
updatedAt Instant
```

Enums:

```java
public enum CashFlowEntryType {
    INCOME,
    EXPENSE
}
```

```java
public enum CashFlowEntryStatus {
    PENDING,
    PAID,
    CANCELLED
}
```

Regras:

- `amount` deve ser maior que zero.
- `description` não pode ser vazia.
- `companyId` é obrigatório.
- `dueDate` é obrigatória.
- `paymentDate` pode ser nula.
- se `status = PAID`, `paymentDate` deve ser informada.
- se `type = INCOME`, soma nas receitas.
- se `type = EXPENSE`, soma nas despesas.

---

## 8. Modelo de dados

### 8.1 Tabela `cashflow_entries`

```sql
CREATE TABLE cashflow_entries (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    description VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    category VARCHAR(100) NOT NULL,
    account VARCHAR(100) NOT NULL,
    due_date DATE NOT NULL,
    payment_date DATE NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

Índices sugeridos:

```sql
CREATE INDEX idx_cashflow_entries_company_due_date
ON cashflow_entries(company_id, due_date);

CREATE INDEX idx_cashflow_entries_company_payment_date
ON cashflow_entries(company_id, payment_date);

CREATE INDEX idx_cashflow_entries_company_status
ON cashflow_entries(company_id, status);
```

### 8.2 Tabela `outbox_events`

```sql
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload CLOB NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempts INT NOT NULL,
    last_error CLOB NULL,
    created_at TIMESTAMP NOT NULL,
    published_at TIMESTAMP NULL,
    updated_at TIMESTAMP NOT NULL
);
```

Enum:

```java
public enum OutboxEventStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
```

Regras:

- evento nasce como `PENDING`;
- ao publicar com sucesso, muda para `PUBLISHED`;
- se falhar, incrementa `attempts`;
- se `attempts >= maxAttempts`, muda para `FAILED`.

---

## 9. Eventos de domínio

### 9.1 Interface base

```java
public interface DomainEvent {
    UUID eventId();
    UUID aggregateId();
    String eventType();
    Instant occurredAt();
}
```

### 9.2 Evento `CashFlowEntryCreatedEvent`

```java
public record CashFlowEntryCreatedEvent(
    UUID eventId,
    UUID aggregateId,
    UUID companyId,
    String description,
    CashFlowEntryType type,
    BigDecimal amount,
    String category,
    String account,
    LocalDate dueDate,
    LocalDate paymentDate,
    CashFlowEntryStatus status,
    Instant occurredAt
) implements DomainEvent {

    @Override
    public String eventType() {
        return "CashFlowEntryCreated";
    }
}
```

Esse evento deve ser serializado como JSON no campo `payload` da tabela `outbox_events`.

---

## 10. Interfaces principais

### 10.1 CashFlowEntryRepository

```java
public interface CashFlowEntryRepository {
    CashFlowEntry save(CashFlowEntry entry);
    List<CashFlowEntry> findByCompanyIdAndMonth(UUID companyId, YearMonth month);
    Optional<CashFlowEntry> findById(UUID id);
}
```

Implementação esperada:

```text
JpaCashFlowEntryRepository
SpringDataCashFlowEntryJpaRepository
```

### 10.2 OutboxRepository

```java
public interface OutboxRepository {
    OutboxEvent save(OutboxEvent event);
    List<OutboxEvent> findPending(int limit);
    List<OutboxEvent> findFailed(int limit);
    void markAsPublished(UUID eventId);
    void markAsFailed(UUID eventId, String errorMessage);
    void incrementAttempts(UUID eventId, String errorMessage);
    OutboxStats getStats();
    void resetFailedToPending();
}
```

### 10.3 EventBus

```java
public interface EventBus {
    void publish(DomainEvent event);
    Optional<DomainEvent> poll();
}
```

Implementação local:

```text
InMemoryEventBus
```

Internamente, usar:

```java
BlockingQueue<DomainEvent>
```

### 10.4 CashFlowReadModelRepository

```java
public interface CashFlowReadModelRepository {
    Optional<CashFlowMonthlySummaryView> findMonthlySummary(UUID companyId, YearMonth month);
    void upsert(CashFlowMonthlySummaryView summary);
    void clear();
    Map<String, CashFlowMonthlySummaryView> findAll();
}
```

Implementação local:

```text
InMemoryCashFlowReadModelRepository
```

Internamente, usar:

```java
ConcurrentHashMap<String, CashFlowMonthlySummaryView>
```

Chave:

```text
companyId + ":" + yyyy-MM
```

---

## 11. Endpoints obrigatórios

### 11.1 Criar lançamento

```http
POST /api/cashflow-entries
```

Request:

```json
{
  "companyId": "11111111-1111-1111-1111-111111111111",
  "description": "Venda de serviço",
  "type": "INCOME",
  "amount": 1500.00,
  "category": "SALES",
  "account": "MAIN_ACCOUNT",
  "dueDate": "2026-06-10",
  "paymentDate": "2026-06-10",
  "status": "PAID"
}
```

Response:

```json
{
  "id": "uuid",
  "companyId": "uuid",
  "description": "Venda de serviço",
  "type": "INCOME",
  "amount": 1500.00,
  "category": "SALES",
  "account": "MAIN_ACCOUNT",
  "dueDate": "2026-06-10",
  "paymentDate": "2026-06-10",
  "status": "PAID",
  "createdAt": "2026-06-03T10:00:00Z"
}
```

### 11.2 Listar lançamentos

```http
GET /api/cashflow-entries?companyId={companyId}&month=2026-06
```

Deve retornar os lançamentos oficiais da tabela transacional.

### 11.3 Consultar resumo mensal

```http
GET /api/cashflow-summary/monthly?companyId={companyId}&month=2026-06
```

Response:

```json
{
  "companyId": "11111111-1111-1111-1111-111111111111",
  "month": "2026-06",
  "totalIncome": 10000.00,
  "totalExpense": 6500.00,
  "projectedBalance": 3500.00,
  "paidIncome": 8000.00,
  "paidExpense": 5000.00,
  "realizedBalance": 3000.00,
  "entriesCount": 12,
  "lastUpdatedAt": "2026-06-03T10:01:00Z",
  "source": "READ_MODEL"
}
```

`source` pode ser:

```text
READ_MODEL
FALLBACK_RECALCULATION
```

### 11.4 Diagnóstico da outbox

```http
GET /api/diagnostics/outbox
```

Response:

```json
{
  "pending": 2,
  "published": 15,
  "failed": 1
}
```

### 11.5 Reprocessar eventos falhos

```http
POST /api/diagnostics/outbox/reprocess-failed
```

Comportamento:

- buscar eventos com status `FAILED`;
- alterar status para `PENDING`;
- zerar ou preservar attempts, conforme decisão simples;
- permitir que o `OutboxPublisher` tente novamente.

### 11.6 Consultar read model

```http
GET /api/diagnostics/read-model
```

Retornar estado atual do read model em memória.

### 11.7 Limpar read model

```http
POST /api/diagnostics/read-model/clear
```

Limpa o read model para permitir demonstrar fallback.

### 11.8 Simular falha no worker

```http
POST /api/diagnostics/projection/failure-mode
```

Request:

```json
{
  "enabled": true
}
```

Quando habilitado, o worker deve falhar ao processar eventos.

### 11.9 Health check

```http
GET /actuator/health
```

---

## 12. Fluxo de escrita

Fluxo esperado:

```text
POST /api/cashflow-entries
        |
        v
CreateCashFlowEntryController
        |
        v
CreateCashFlowEntryUseCase
        |
        +--> valida comando
        |
        +--> cria entidade CashFlowEntry
        |
        +--> salva CashFlowEntry
        |
        +--> cria CashFlowEntryCreatedEvent
        |
        +--> salva OutboxEvent
        |
        v
retorna lançamento criado
```

Pseudo-código:

```java
@Transactional
public CashFlowEntryResponse execute(CreateCashFlowEntryCommand command) {
    CashFlowEntry entry = CashFlowEntry.create(...);

    cashFlowEntryRepository.save(entry);

    CashFlowEntryCreatedEvent event = CashFlowEntryCreatedEvent.from(entry);

    OutboxEvent outboxEvent = OutboxEvent.fromDomainEvent(event);

    outboxRepository.save(outboxEvent);

    return CashFlowEntryResponse.from(entry);
}
```

Obrigatório: lançamento e evento de outbox devem ser persistidos na mesma transação.

---

## 13. Outbox Publisher

Implementar componente agendado:

```java
@Component
public class OutboxPublisher {

    @Scheduled(fixedDelayString = "${app.outbox.publisher-delay-ms:2000}")
    public void publishPendingEvents() {
        // buscar eventos PENDING
        // converter payload JSON para DomainEvent
        // publicar no EventBus
        // marcar como PUBLISHED
        // em caso de erro, incrementar attempts ou marcar FAILED
    }
}
```

Comportamento:

1. Buscar até `batchSize` eventos `PENDING`.
2. Para cada evento:
   - desserializar JSON;
   - publicar no `EventBus`;
   - marcar como `PUBLISHED`.
3. Se falhar:
   - incrementar `attempts`;
   - salvar `lastError`;
   - se passar de `maxAttempts`, marcar como `FAILED`.

---

## 14. InMemoryEventBus

Implementar fila local em memória.

```java
@Component
public class InMemoryEventBus implements EventBus {

    private final BlockingQueue<DomainEvent> queue = new LinkedBlockingQueue<>();

    @Override
    public void publish(DomainEvent event) {
        queue.offer(event);
    }

    @Override
    public Optional<DomainEvent> poll() {
        return Optional.ofNullable(queue.poll());
    }
}
```

Esse componente representa conceitualmente Kafka, SQS ou RabbitMQ em uma versão local simplificada.

---

## 15. Projection Worker

Implementar componente agendado:

```java
@Component
public class CashFlowProjectionWorker {

    @Scheduled(fixedDelayString = "${app.projection.worker-delay-ms:1000}")
    public void processEvents() {
        // consumir eventos do EventBus
        // aplicar projeção
        // atualizar read model
    }
}
```

Comportamento:

1. Consumir eventos da fila em memória.
2. Identificar tipo do evento.
3. Para `CashFlowEntryCreatedEvent`, atualizar resumo mensal.
4. Garantir idempotência mínima.
5. Respeitar modo de falha simulada.

---

## 16. Idempotência da projeção

Mesmo em memória, implementar idempotência básica.

No read model ou worker, manter:

```java
private final Set<UUID> processedEventIds = ConcurrentHashMap.newKeySet();
```

Antes de aplicar:

```java
if (processedEventIds.contains(event.eventId())) {
    return;
}
```

Depois de aplicar:

```java
processedEventIds.add(event.eventId());
```

Em produção, isso poderia virar uma tabela `processed_projection_events`.

---

## 17. Read model

Classe esperada:

```java
public class CashFlowMonthlySummaryView {
    private UUID companyId;
    private YearMonth month;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal projectedBalance;
    private BigDecimal paidIncome;
    private BigDecimal paidExpense;
    private BigDecimal realizedBalance;
    private int entriesCount;
    private Instant lastUpdatedAt;
}
```

Regras de cálculo:

- se `type = INCOME`, soma em `totalIncome`;
- se `type = EXPENSE`, soma em `totalExpense`;
- `projectedBalance = totalIncome - totalExpense`;
- se `status = PAID` e `type = INCOME`, soma em `paidIncome`;
- se `status = PAID` e `type = EXPENSE`, soma em `paidExpense`;
- `realizedBalance = paidIncome - paidExpense`;
- incrementar `entriesCount`;
- o mês deve ser calculado por `dueDate`.

```java
YearMonth month = YearMonth.from(dueDate);
```

---

## 18. Consulta com fallback

Use case esperado:

```java
public CashFlowMonthlySummaryResponse getMonthlySummary(UUID companyId, YearMonth month) {
    Optional<CashFlowMonthlySummaryView> view =
        readModelRepository.findMonthlySummary(companyId, month);

    if (view.isPresent()) {
        return CashFlowMonthlySummaryResponse.from(view.get(), "READ_MODEL");
    }

    CashFlowMonthlySummaryView recalculated =
        recalculationService.recalculate(companyId, month);

    readModelRepository.upsert(recalculated);

    return CashFlowMonthlySummaryResponse.from(recalculated, "FALLBACK_RECALCULATION");
}
```

O fallback deve usar a fonte da verdade: tabela `cashflow_entries`.

---

## 19. Rate limit local

Implementar rate limit por cliente usando header:

```http
X-Client-Id: cliente-123
```

Se o header não existir, usar:

```text
anonymous
```

Regra padrão:

```text
60 requests por minuto por clientId
```

Interface:

```java
public interface RateLimiter {
    boolean allow(String clientId);
}
```

Implementação:

```text
InMemoryFixedWindowRateLimiter
```

Estrutura interna:

```java
Map<String, RateLimitWindow>
```

Classe:

```java
public class RateLimitWindow {
    private Instant windowStart;
    private int count;
}
```

Comportamento:

- se janela expirou, reinicia;
- se `count < limit`, permite;
- caso contrário, bloqueia com HTTP 429.

Response de erro:

```json
{
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Too many requests. Try again later."
}
```

---

## 20. Resiliência mínima

Implementar:

### 20.1 Retry no OutboxPublisher

- máximo de tentativas configurável;
- erro salvo em `lastError`;
- status `FAILED` após limite.

### 20.2 Reprocessamento manual

Endpoint:

```http
POST /api/diagnostics/outbox/reprocess-failed
```

### 20.3 Modo de falha da projeção

Quando ativado, `ProjectionWorker` deve lançar exceção ao processar evento.

### 20.4 Health check

Usar Spring Actuator.

---

## 21. Logs obrigatórios

Gerar logs claros nos seguintes pontos.

Na criação do lançamento:

```text
CashFlow entry created: entryId={}, companyId={}, amount={}, type={}
Outbox event created: eventId={}, eventType={}
```

No publisher:

```text
Publishing outbox event: eventId={}, eventType={}, attempt={}
Outbox event published: eventId={}
Outbox event failed: eventId={}, error={}
```

No worker:

```text
Processing domain event: eventId={}, eventType={}
Monthly summary updated: companyId={}, month={}
```

No fallback:

```text
Read model not found. Recalculating summary from transactional database. companyId={}, month={}
```

---

## 22. Fluxos de demonstração

### Cenário 1 — criação e projeção assíncrona

1. Subir aplicação.
2. Criar lançamento com `POST /api/cashflow-entries`.
3. Consultar outbox.
4. Ver evento `PENDING` ou `PUBLISHED`.
5. Aguardar worker.
6. Consultar resumo mensal.
7. Ver consolidado atualizado.

### Cenário 2 — fallback

1. Criar lançamento.
2. Limpar read model.
3. Chamar resumo mensal.
4. Response deve vir com:

```json
"source": "FALLBACK_RECALCULATION"
```

### Cenário 3 — falha na projeção

1. Ativar failure mode.
2. Criar lançamento.
3. Outbox publica evento.
4. Worker falha ao processar.
5. Logs mostram erro.
6. Desativar failure mode.
7. Reprocessar ou aguardar nova tentativa, conforme implementação.

### Cenário 4 — rate limit

1. Fazer várias requisições com mesmo `X-Client-Id`.
2. Após limite, API retorna HTTP 429.

---

## 23. Critérios de aceite

A implementação será considerada suficiente quando:

- a aplicação sobe localmente sem Docker;
- é possível criar um lançamento financeiro;
- o lançamento é salvo no banco;
- um evento é salvo na outbox na mesma transação;
- o publisher publica eventos pendentes;
- o worker consome eventos;
- o read model é atualizado;
- a consulta mensal retorna dados consolidados;
- existe fallback para recálculo;
- existe rate limit simples;
- existem endpoints de diagnóstico;
- logs demonstram o fluxo;
- o código tem separação clara entre domínio, aplicação e infraestrutura;
- existe README com instruções de execução e exemplos curl.

---

## 24. O que não implementar agora

Não implementar nesta POC:

- autenticação JWT real;
- autorização por perfil;
- multi-tenant robusto;
- frontend;
- AWS;
- Docker;
- Kafka;
- SQS;
- DynamoDB;
- Redis;
- DLQ real;
- OpenTelemetry;
- tracing distribuído;
- CI/CD;
- testes de carga complexos;
- conciliação bancária;
- importação de extratos.

---

## 25. Evolução futura

A arquitetura deve permitir substituições futuras:

```text
H2
  -> PostgreSQL

InMemoryEventBus
  -> Kafka / SQS / RabbitMQ

InMemoryCashFlowReadModelRepository
  -> DynamoDB / Redis / PostgreSQL materialized table

InMemoryRateLimiter
  -> Redis-based rate limiter / API Gateway

@Scheduled OutboxPublisher
  -> Debezium CDC / worker dedicado

Monólito modular
  -> serviços separados
```

---

## 26. Ordem de implementação recomendada

### Fase 1 — Projeto base

Criar projeto Spring Boot com:

- Web;
- JPA;
- H2;
- Validation;
- Actuator;
- Scheduler.

Criar estrutura de pacotes.

### Fase 2 — Lançamentos

Implementar:

- entidade `CashFlowEntry`;
- repository;
- command;
- use case;
- controller;
- endpoint `POST /api/cashflow-entries`;
- endpoint `GET /api/cashflow-entries`.

### Fase 3 — Outbox

Implementar:

- entidade `OutboxEvent`;
- repository;
- serialização JSON;
- criação de evento junto com lançamento;
- endpoint de diagnóstico da outbox.

### Fase 4 — EventBus e Publisher

Implementar:

- interface `EventBus`;
- `InMemoryEventBus`;
- `OutboxPublisher`;
- logs de publicação;
- retry básico.

### Fase 5 — Worker e Read Model

Implementar:

- `CashFlowMonthlySummaryView`;
- `CashFlowReadModelRepository`;
- `InMemoryCashFlowReadModelRepository`;
- `ProjectionWorker`;
- atualização do resumo mensal.

### Fase 6 — Consulta e fallback

Implementar:

- endpoint `GET /api/cashflow-summary/monthly`;
- busca no read model;
- fallback recalculando a partir do banco;
- campo `source`.

### Fase 7 — Diagnóstico e simulações

Implementar:

- limpar read model;
- ativar/desativar falha no worker;
- reprocessar outbox failed;
- endpoint de status.

### Fase 8 — Rate limit

Implementar:

- filtro HTTP;
- header `X-Client-Id`;
- rate limiter em memória;
- resposta HTTP 429.

### Fase 9 — Testes mínimos

Criar testes para:

- criação de lançamento;
- criação de outbox;
- publicação de outbox;
- atualização do read model;
- fallback;
- rate limit.

---

## 27. README esperado

Criar README com:

```text
# Cash Flow Architecture POC

## Objetivo

POC local para demonstrar arquitetura de fluxo de caixa com:
- escrita transacional;
- outbox;
- event bus em memória;
- worker de projeção;
- read model materializado;
- fallback;
- rate limit;
- resiliência básica.

## Como rodar

./mvnw spring-boot:run

## H2 Console

http://localhost:8080/h2-console

JDBC URL:
jdbc:h2:file:./data/cashflow

## Criar lançamento

curl -X POST http://localhost:8080/api/cashflow-entries \
  -H "Content-Type: application/json" \
  -H "X-Client-Id: demo-client" \
  -d '{
    "companyId": "11111111-1111-1111-1111-111111111111",
    "description": "Venda de serviço",
    "type": "INCOME",
    "amount": 1500.00,
    "category": "SALES",
    "account": "MAIN_ACCOUNT",
    "dueDate": "2026-06-10",
    "paymentDate": "2026-06-10",
    "status": "PAID"
  }'

## Consultar resumo mensal

curl "http://localhost:8080/api/cashflow-summary/monthly?companyId=11111111-1111-1111-1111-111111111111&month=2026-06"

## Consultar outbox

curl http://localhost:8080/api/diagnostics/outbox

## Limpar read model

curl -X POST http://localhost:8080/api/diagnostics/read-model/clear

## Ativar falha simulada no worker

curl -X POST http://localhost:8080/api/diagnostics/projection/failure-mode \
  -H "Content-Type: application/json" \
  -d '{"enabled": true}'
```

---

## 28. Texto para defesa arquitetural

A implementação local foi feita como monólito modular para reduzir dependências operacionais e permitir validação rápida em uma máquina pessoal. Apesar disso, os componentes foram separados por contratos. A fila, o read model e o rate limiter usam implementações em memória, mas podem ser substituídos por Kafka/SQS, DynamoDB/Redis e API Gateway/Redis sem alterar o domínio ou os casos de uso principais.

A fonte da verdade dos lançamentos financeiros é a base transacional. O read model é uma projeção derivada, otimizada para consulta de fluxo de caixa por empresa e mês. Caso essa projeção esteja ausente ou inconsistente, ela pode ser reconstruída a partir dos lançamentos oficiais.

A Outbox garante que o lançamento e o evento sejam gravados na mesma transação. Assim, evita-se o problema clássico de salvar no banco e falhar ao publicar na fila, ou publicar na fila e falhar ao salvar no banco.
