# Cash Flow Architecture POC

POC local para demonstrar arquitetura de fluxo de caixa com:

- escrita transacional;
- outbox pattern;
- event bus em memoria;
- worker de projecao;
- read model materializado;
- fallback de consulta;
- rate limit local;
- resiliencia minima.

## Como rodar

```bash
mvn spring-boot:run
```

## H2 Console

URL: `http://localhost:8080/h2-console`

JDBC URL:

```text
jdbc:h2:file:./data/cashflow
```

Usuario: `sa`

Senha: vazia

## Criar lancamento

```bash
curl -X POST http://localhost:8080/api/cashflow-entries \
  -H "Content-Type: application/json" \
  -H "X-Client-Id: demo-client" \
  -d '{
    "companyId": "11111111-1111-1111-1111-111111111111",
    "description": "Venda de servico",
    "type": "INCOME",
    "amount": 1500.00,
    "category": "SALES",
    "account": "MAIN_ACCOUNT",
    "dueDate": "2026-06-10",
    "paymentDate": "2026-06-10",
    "status": "PAID"
  }'
```

## Listar lancamentos

```bash
curl "http://localhost:8080/api/cashflow-entries?companyId=11111111-1111-1111-1111-111111111111&month=2026-06"
```

## Consultar resumo mensal

```bash
curl "http://localhost:8080/api/cashflow-summary/monthly?companyId=11111111-1111-1111-1111-111111111111&month=2026-06"
```

## Diagnosticos

```bash
curl http://localhost:8080/api/diagnostics/outbox
curl http://localhost:8080/api/diagnostics/read-model
curl -X POST http://localhost:8080/api/diagnostics/read-model/clear
curl -X POST http://localhost:8080/api/diagnostics/outbox/reprocess-failed
curl -X POST http://localhost:8080/api/diagnostics/projection/failure-mode \
  -H "Content-Type: application/json" \
  -d '{"enabled": true}'
```
=======
# cashflow
Desafio Sistema de Fluxo de Caixa