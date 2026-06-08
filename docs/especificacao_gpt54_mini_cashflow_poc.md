# Especificação para GPT 5.4 Mini — Complementação da POC de Fluxo de Caixa

## 1. Papel do agente

Você é um desenvolvedor Java auxiliar. Sua tarefa é completar partes faltantes de um projeto Spring Boot já iniciado, seguindo exatamente a arquitetura definida.

Você deve priorizar código simples, funcional, compilável e coerente com o desenho existente.

Não altere a arquitetura geral sem necessidade.

---

## 2. Contexto do projeto

O projeto é uma POC local de arquitetura de fluxo de caixa.

Ele roda como monólito modular Spring Boot, sem Docker, sem AWS, sem Kafka, sem Redis e sem DynamoDB.

O objetivo é demonstrar:

- criação de lançamentos financeiros;
- persistência em banco local H2;
- Outbox Pattern;
- publicação de eventos em fila em memória;
- worker de projeção;
- read model em memória;
- consulta com fallback;
- rate limit em memória;
- endpoints de diagnóstico.

---

## 3. Regras obrigatórias

Siga estas regras:

- Não misture responsabilidades entre pacotes.
- Não coloque regra de negócio em controllers.
- Não acesse implementação concreta se existir interface.
- Preserve os nomes de classes, pacotes e contratos existentes.
- Complete apenas o que estiver faltando.
- Prefira soluções simples a abstrações excessivas.
- Mantenha o projeto compilando.
- Não adicione infraestrutura externa.
- Não adicione dependências pesadas sem necessidade.

Não adicionar:

- Docker;
- Kafka;
- Redis;
- AWS;
- DynamoDB;
- autenticação JWT;
- frontend;
- Kubernetes;
- Debezium;
- OpenTelemetry.

---

## 4. Prioridade de trabalho

Siga esta ordem:

1. Fazer o projeto compilar.
2. Completar DTOs de request e response.
3. Completar mappers.
4. Completar repositories JPA.
5. Completar use cases.
6. Completar endpoints faltantes.
7. Completar serialização e desserialização de eventos.
8. Completar worker e read model.
9. Completar rate limit.
10. Criar testes unitários simples.
11. Melhorar logs e mensagens de erro.
12. Atualizar README se necessário.

---

## 5. Estrutura esperada de pacotes

Respeite a estrutura existente ou use esta como referência:

```text
src/main/java/com/example/cashflow

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

---

## 6. Modelo principal

### 6.1 CashFlowEntry

Campos esperados:

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

---

## 7. DTOs esperados

### 7.1 CreateCashFlowEntryRequest

```java
public class CreateCashFlowEntryRequest {
    private UUID companyId;
    private String description;
    private CashFlowEntryType type;
    private BigDecimal amount;
    private String category;
    private String account;
    private LocalDate dueDate;
    private LocalDate paymentDate;
    private CashFlowEntryStatus status;
}
```

Adicionar validações Bean Validation:

- `@NotNull` em `companyId`, `type`, `amount`, `dueDate`, `status`;
- `@NotBlank` em `description`, `category`, `account`;
- `@DecimalMin(value = "0.01")` em `amount`.

### 7.2 CashFlowEntryResponse

```java
public class CashFlowEntryResponse {
    private UUID id;
    private UUID companyId;
    private String description;
    private CashFlowEntryType type;
    private BigDecimal amount;
    private String category;
    private String account;
    private LocalDate dueDate;
    private LocalDate paymentDate;
    private CashFlowEntryStatus status;
    private Instant createdAt;
}
```

### 7.3 CashFlowMonthlySummaryResponse

```java
public class CashFlowMonthlySummaryResponse {
    private UUID companyId;
    private String month;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal projectedBalance;
    private BigDecimal paidIncome;
    private BigDecimal paidExpense;
    private BigDecimal realizedBalance;
    private int entriesCount;
    private Instant lastUpdatedAt;
    private String source;
}
```

### 7.4 FailureModeRequest

```java
public class FailureModeRequest {
    private boolean enabled;
}
```

### 7.5 ErrorResponse

```java
public class ErrorResponse {
    private String error;
    private String message;
    private Instant timestamp;
}
```

---

## 8. Endpoints a completar

### 8.1 Criar lançamento

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

### 8.2 Listar lançamentos

```http
GET /api/cashflow-entries?companyId={companyId}&month=2026-06
```

### 8.3 Consultar resumo mensal

```http
GET /api/cashflow-summary/monthly?companyId={companyId}&month=2026-06
```

### 8.4 Diagnóstico da outbox

```http
GET /api/diagnostics/outbox
```

### 8.5 Reprocessar eventos falhos

```http
POST /api/diagnostics/outbox/reprocess-failed
```

### 8.6 Consultar read model

```http
GET /api/diagnostics/read-model
```

### 8.7 Limpar read model

```http
POST /api/diagnostics/read-model/clear
```

### 8.8 Ativar/desativar falha simulada

```http
POST /api/diagnostics/projection/failure-mode
```

Request:

```json
{
  "enabled": true
}
```

---

## 9. Mappers

Criar ou completar mappers simples.

### 9.1 CashFlowEntryMapper

Responsabilidades:

- converter `CreateCashFlowEntryRequest` para command;
- converter domínio para response;
- converter entidade JPA para domínio, se o projeto separar JPA entity de domínio;
- converter domínio para JPA entity, se necessário.

Não colocar regra de negócio complexa no mapper.

---

## 10. Repositories

### 10.1 CashFlowEntryRepository

Contrato esperado:

```java
public interface CashFlowEntryRepository {
    CashFlowEntry save(CashFlowEntry entry);
    List<CashFlowEntry> findByCompanyIdAndMonth(UUID companyId, YearMonth month);
    Optional<CashFlowEntry> findById(UUID id);
}
```

Na implementação JPA, para buscar por mês, usar intervalo:

```java
LocalDate start = month.atDay(1);
LocalDate end = month.plusMonths(1).atDay(1);
```

Buscar:

```text
dueDate >= start AND dueDate < end
```

### 10.2 OutboxRepository

Contrato esperado:

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

Completar implementação de forma simples e funcional.

---

## 11. Evento de domínio

### 11.1 DomainEvent

```java
public interface DomainEvent {
    UUID eventId();
    UUID aggregateId();
    String eventType();
    Instant occurredAt();
}
```

### 11.2 CashFlowEntryCreatedEvent

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

Completar factory methods se necessário:

```java
public static CashFlowEntryCreatedEvent from(CashFlowEntry entry) { ... }
```

---

## 12. Serialização de eventos

Criar ou completar serviço simples:

```java
public class DomainEventJsonSerializer {
    String serialize(DomainEvent event);
    DomainEvent deserialize(String eventType, String payload);
}
```

Regras:

- usar Jackson/ObjectMapper;
- se `eventType = CashFlowEntryCreated`, desserializar para `CashFlowEntryCreatedEvent`;
- se tipo desconhecido, lançar exceção clara.

---

## 13. Outbox Publisher

Completar comportamento:

```text
A cada 2 segundos:
  buscar eventos PENDING
  desserializar payload
  publicar no EventBus
  marcar como PUBLISHED
  em erro, incrementar attempts
  se attempts >= maxAttempts, marcar FAILED
```

Cuidados:

- logar cada tentativa;
- não quebrar o loop inteiro se um evento falhar;
- tratar exceção por evento;
- respeitar batch size configurável.

---

## 14. EventBus em memória

Completar implementação:

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

Não usar Kafka ou bibliotecas externas de fila.

---

## 15. Read model

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

Completar métodos úteis:

```java
public void apply(CashFlowEntryCreatedEvent event) { ... }
public static CashFlowMonthlySummaryView empty(UUID companyId, YearMonth month) { ... }
```

Regras:

- `INCOME` soma em `totalIncome`;
- `EXPENSE` soma em `totalExpense`;
- `projectedBalance = totalIncome - totalExpense`;
- se `status = PAID` e `type = INCOME`, soma em `paidIncome`;
- se `status = PAID` e `type = EXPENSE`, soma em `paidExpense`;
- `realizedBalance = paidIncome - paidExpense`;
- incrementar `entriesCount`.

---

## 16. Projection Worker

Completar worker agendado.

Comportamento:

```text
A cada 1 segundo:
  consumir até N eventos do EventBus
  se failure mode estiver ativo, lançar exceção simulada
  se evento já foi processado, ignorar
  aplicar evento no read model
  registrar eventId como processado
```

Idempotência em memória:

```java
private final Set<UUID> processedEventIds = ConcurrentHashMap.newKeySet();
```

Não implementar tabela de idempotência agora.

---

## 17. Consulta com fallback

Completar use case de resumo mensal.

Comportamento:

```text
1. Buscar read model por companyId e month.
2. Se existir, retornar source = READ_MODEL.
3. Se não existir, buscar lançamentos no banco transacional.
4. Recalcular resumo.
5. Salvar no read model.
6. Retornar source = FALLBACK_RECALCULATION.
```

O fallback deve usar `CashFlowEntryRepository`, não a outbox.

---

## 18. Rate limit

Completar rate limit simples por header:

```http
X-Client-Id: cliente-123
```

Se header não existir:

```text
anonymous
```

Implementar filtro HTTP.

Regra padrão:

```text
60 requests por minuto
```

Response quando exceder:

```json
{
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Too many requests. Try again later.",
  "timestamp": "2026-06-03T10:00:00Z"
}
```

Status HTTP:

```text
429 Too Many Requests
```

---

## 19. Tratamento de erros

Criar ou completar `@ControllerAdvice` global.

Tratar pelo menos:

- validação Bean Validation;
- argumentos inválidos;
- erros de negócio;
- erro genérico.

Response padrão:

```json
{
  "error": "VALIDATION_ERROR",
  "message": "Descrição do erro",
  "timestamp": "2026-06-03T10:00:00Z"
}
```

---

## 20. Logs

Adicionar logs em pontos-chave.

Criação:

```text
CashFlow entry created: entryId={}, companyId={}, amount={}, type={}
Outbox event created: eventId={}, eventType={}
```

Publisher:

```text
Publishing outbox event: eventId={}, eventType={}, attempt={}
Outbox event published: eventId={}
Outbox event failed: eventId={}, error={}
```

Worker:

```text
Processing domain event: eventId={}, eventType={}
Monthly summary updated: companyId={}, month={}
```

Fallback:

```text
Read model not found. Recalculating summary from transactional database. companyId={}, month={}
```

---

## 21. Testes mínimos

Criar testes simples para:

### 21.1 CashFlowMonthlySummaryViewTest

Validar:

- income soma receita;
- expense soma despesa;
- paid income soma paidIncome;
- paid expense soma paidExpense;
- balances são recalculados corretamente.

### 21.2 CreateCashFlowEntryUseCaseTest

Validar:

- cria lançamento;
- cria evento outbox;
- falha se amount <= 0;
- falha se status PAID sem paymentDate.

### 21.3 InMemoryRateLimiterTest

Validar:

- permite até limite;
- bloqueia após limite;
- reinicia após nova janela, se fácil de testar.

### 21.4 CashFlowSummaryQueryUseCaseTest

Validar:

- retorna READ_MODEL quando existe;
- retorna FALLBACK_RECALCULATION quando não existe.

Não criar testes complexos de integração se isso atrasar demais.

---

## 22. README

Se o README estiver ausente ou incompleto, adicionar comandos básicos:

```text
./mvnw spring-boot:run
```

H2 Console:

```text
http://localhost:8080/h2-console
jdbc:h2:file:./data/cashflow
```

Exemplo de criação:

```bash
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
```

Exemplo de consulta:

```bash
curl "http://localhost:8080/api/cashflow-summary/monthly?companyId=11111111-1111-1111-1111-111111111111&month=2026-06"
```

---

## 23. Critérios de conclusão da sua tarefa

Sua tarefa estará concluída quando:

- o projeto compilar;
- os endpoints principais responderem;
- for possível criar lançamento;
- o lançamento gerar evento na outbox;
- o publisher publicar evento;
- o worker atualizar read model;
- a consulta mensal funcionar;
- o fallback funcionar;
- o rate limit funcionar;
- existirem logs claros;
- testes básicos passarem ou estiverem criados de forma coerente.

---

## 24. Lembrete importante

Esta é uma POC arquitetural. Não tente transformar em solução enterprise completa.

Prefira:

```text
simples + funcional + bem separado
```

Em vez de:

```text
complexo + incompleto + difícil de rodar
```
