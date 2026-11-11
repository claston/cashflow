# Referência da API

[Início](../README.md) · [Visão da solução](solucao_fluxo_caixa_consolidada.md) · [Arquitetura da POC](arquitetura_poc.md) · [Guia de execução](guia_execucao.md)

Base local: `http://localhost:8080`. Os contratos abaixo descrevem os controllers e DTOs da implementação atual. As datas e os identificadores nos exemplos são ilustrativos. Os comandos completos para Bash e PowerShell estão no [guia de execução](guia_execucao.md#demonstracao).

## Conteúdo

- [Convenções e cabeçalhos](#convencoes-e-cabecalhos)
- [Endpoints](#endpoints)
- [Criar lançamento](#criar-lancamento)
- [Listar lançamentos](#listar-lancamentos)
- [Consultar resumo mensal](#consultar-resumo-mensal)
- [Regras do resumo](#regras-do-resumo)
- [Diagnósticos](#diagnosticos)
- [Erros](#erros)

<a id="convencoes-e-cabecalhos"></a>

## Convenções e cabeçalhos

| Item | Convenção |
| --- | --- |
| Corpo das requisições e respostas | JSON; envie `Content-Type: application/json` nas requisições com corpo |
| Identificadores | UUID |
| Datas de vencimento e pagamento | `YYYY-MM-DD` |
| Mês consultado | `YYYY-MM` |
| Instantes gerados pelo sistema | ISO 8601, como `2026-06-10T12:00:00Z` |
| Valores | Números JSON positivos na criação; use duas casas decimais nos exemplos monetários |
| `X-Client-Id` | Opcional; chave de rate limit. Ausente ou vazio utiliza a chave compartilhada `anonymous` |
| `X-Correlation-Id` | Opcional; o filtro HTTP utiliza o valor recebido ou gera um UUID e o devolve no cabeçalho da resposta |

O limite padrão é de **60 requisições por janela de 60 segundos por cliente**, compartilhado entre as rotas `/api/`, inclusive diagnósticos. A janela começa no primeiro uso da chave e é renovada quando expira. `/actuator/health` e `/h2-console` ficam fora desse filtro.

`X-Client-Id` e `companyId` não são credenciais. A POC não autentica usuários nem verifica se o solicitante pode acessar uma empresa. A correlação HTTP não é propagada automaticamente ao processamento assíncrono.

<a id="endpoints"></a>

## Endpoints

| Método | Caminho | Sucesso | Resultado |
| --- | --- | --- | --- |
| POST | `/api/cashflow-entries` | 200 | Lançamento criado |
| GET | `/api/cashflow-entries` | 200 | Lista de lançamentos da empresa e mês |
| GET | `/api/cashflow-summary/monthly` | 200 | Resumo mensal |
| GET | `/api/diagnostics/outbox` | 200 | Contagens de eventos por status |
| POST | `/api/diagnostics/outbox/reprocess-failed` | 200 | Contagens após solicitar reprocessamento |
| GET | `/api/diagnostics/read-model` | 200 | Mapa dos resumos em memória |
| POST | `/api/diagnostics/read-model/clear` | 200 | Corpo vazio |
| POST | `/api/diagnostics/projection/failure-mode` | 200 | Estado da simulação de falha |
| GET | `/actuator/health` | 200 quando saudável | Saúde básica da aplicação |

O POST de criação retorna **200** na implementação atual. Não há endpoints de atualização, exclusão, consulta individual por ID ou consolidado diário.

<a id="criar-lancamento"></a>

## Criar lançamento

`POST /api/cashflow-entries`

| Campo | Tipo | Obrigatório | Regra |
| --- | --- | --- | --- |
| `companyId` | UUID | Sim | Identifica a empresa; não exige cadastro prévio na POC |
| `description` | String | Sim | Não pode ser vazia ou conter apenas espaços |
| `type` | Enum | Sim | `INCOME` ou `EXPENSE` |
| `amount` | Número decimal | Sim | Mínimo de `0.01` na validação HTTP |
| `category` | String | Sim | Não vazia; texto livre, sem catálogo de categorias |
| `account` | String | Sim | Não vazia; texto livre, sem cadastro de contas |
| `dueDate` | Data | Sim | Define o mês utilizado na listagem e na consolidação |
| `paymentDate` | Data | Condicional | Pode ser nula, mas é exigida quando `status = PAID` |
| `status` | Enum | Sim | `PENDING`, `PAID` ou `CANCELLED` |

O banco armazena `amount` com precisão 19 e escala 2. Não existe campo de moeda nem validação HTTP específica para rejeitar mais de duas casas decimais. Não há regra que exija que `paymentDate` seja posterior a `dueDate`.

Exemplo de requisição:

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

Exemplo de resposta:

```json
{
  "id": "22222222-2222-4222-8222-222222222222",
  "companyId": "11111111-1111-1111-1111-111111111111",
  "description": "Venda de serviço",
  "type": "INCOME",
  "amount": 1500.00,
  "category": "SALES",
  "account": "MAIN_ACCOUNT",
  "dueDate": "2026-06-10",
  "paymentDate": "2026-06-10",
  "status": "PAID",
  "createdAt": "2026-06-10T12:00:00Z"
}
```

`id` e `createdAt` são gerados pelo sistema. O lançamento e sua outbox são gravados antes da resposta; a projeção é posterior. Repetir a requisição cria outro lançamento, pois o endpoint não possui chave de idempotência.

<a id="listar-lancamentos"></a>

## Listar lançamentos

`GET /api/cashflow-entries?companyId={uuid}&month=2026-06`

Os parâmetros `companyId` e `month` são obrigatórios. A consulta busca os registros transacionais com vencimento a partir do primeiro dia do mês, inclusive, até o primeiro dia do mês seguinte, exclusive.

A resposta é um array de objetos com o formato do lançamento retornado na criação. Sem registros, retorna `[]`. Não há paginação nem ordem de resultados definida pelo contrato atual.

<a id="consultar-resumo-mensal"></a>

## Consultar resumo mensal

`GET /api/cashflow-summary/monthly?companyId={uuid}&month=2026-06`

Os parâmetros `companyId` e `month` são obrigatórios. Exemplo com uma receita paga de 1.500 e uma despesa pendente de 300, após a projeção dos dois lançamentos:

```json
{
  "companyId": "11111111-1111-1111-1111-111111111111",
  "month": "2026-06",
  "totalIncome": 1500.00,
  "totalExpense": 300.00,
  "projectedBalance": 1200.00,
  "paidIncome": 1500.00,
  "paidExpense": 0.00,
  "realizedBalance": 1500.00,
  "entriesCount": 2,
  "lastUpdatedAt": "2026-06-10T12:00:03Z",
  "source": "READ_MODEL"
}
```

| `source` | Significado |
| --- | --- |
| `READ_MODEL` | O resumo já existia em memória |
| `FALLBACK_RECALCULATION` | O resumo estava ausente e foi recalculado a partir dos lançamentos e salvo em memória |

`source` informa o caminho da consulta, não comprova que a projeção está completa ou atualizada. `lastUpdatedAt` representa a última aplicação de valores ou criação do resumo, não a data do último pagamento. Não há detecção automática de atraso da projeção.

<a id="regras-do-resumo"></a>

## Regras do resumo

| Campo | Cálculo atual |
| --- | --- |
| `totalIncome` | Soma dos lançamentos `INCOME` do mês de vencimento |
| `totalExpense` | Soma dos lançamentos `EXPENSE` do mês de vencimento |
| `projectedBalance` | `totalIncome - totalExpense` |
| `paidIncome` | Soma dos `INCOME` com status `PAID`, dentro do mesmo mês de vencimento |
| `paidExpense` | Soma dos `EXPENSE` com status `PAID`, dentro do mesmo mês de vencimento |
| `realizedBalance` | `paidIncome - paidExpense` |
| `entriesCount` | Quantidade de lançamentos aplicados ao resumo |

Os totais gerais incluem os três status, inclusive **`CANCELLED`**. Apenas os totais pagos exigem `PAID`. Essa é a regra implementada; a interpretação de cancelamentos permanece uma [questão de negócio em aberto](solucao_fluxo_caixa_consolidada.md#questoes-em-aberto).

O agrupamento sempre usa **`dueDate`**, inclusive para os valores pagos. Um lançamento com vencimento em junho e pagamento em julho integra o resumo de junho. Não há saldo inicial, transporte de saldo entre meses ou consolidação por data de pagamento.

Sem lançamentos e sem projeção prévia, o fallback retorna valores zerados, `entriesCount: 0` e `source: FALLBACK_RECALCULATION`.

<a id="diagnosticos"></a>

## Diagnósticos

### Outbox e reprocessamento

`GET /api/diagnostics/outbox` retorna contagens globais, sem filtro por empresa:

```json
{
  "pending": 0,
  "published": 2,
  "failed": 0
}
```

`PENDING` aguarda publicação; `PUBLISHED` foi enviado à fila; `FAILED` atingiu o limite de falhas de publicação. A resposta não mede o número de eventos consumidos pelo worker.

`POST /api/diagnostics/outbox/reprocess-failed`, sem corpo, retorna o mesmo formato de contagens. Recoloca todos os eventos `FAILED` em `PENDING`, zera tentativas e limpa o último erro. A publicação acontece em um ciclo posterior do publicador. Não é um comando de replay de todos os eventos publicados.

### Modelo de leitura

`GET /api/diagnostics/read-model` retorna um objeto indexado por `companyId:YYYY-MM`. Cada valor contém os campos do resumo, sem o campo `source`. Um modelo vazio retorna `{}`. O endpoint permite observar a projeção sem disparar o fallback.

`POST /api/diagnostics/read-model/clear`, sem corpo, limpa os resumos **de todas as empresas** e retorna 200 com corpo vazio. Lançamentos, outbox, fila e IDs processados pelo worker não são limpos.

### Falha simulada da projeção

`POST /api/diagnostics/projection/failure-mode` recebe e devolve:

```json
{
  "enabled": true
}
```

Use `false` para desativar. Quando ativo, o worker retira um evento, devolve-o à fila e lança a falha simulada. Isso não marca o evento como `FAILED` na outbox. Após desativar, o worker pode processá-lo em outro ciclo, desde que a fila continue no mesmo processo.

Esses endpoints são controles de demonstração da POC e não têm autenticação. Consulte os [limites de recuperação](arquitetura_poc.md#garantias-e-limitacoes) antes de interpretar o resultado.

<a id="erros"></a>

## Erros

Os erros tratados usam `error`, `message` e `timestamp`:

```json
{
  "error": "INVALID_CASHFLOW_ENTRY",
  "message": "paymentDate is required when status is PAID.",
  "timestamp": "2026-06-10T12:00:00Z"
}
```

| HTTP | `error` | Situação tratada |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | Violação das validações dos campos da requisição |
| 400 | `INVALID_REQUEST` | JSON malformado ou corpo incompatível com o DTO |
| 400 | `INVALID_CASHFLOW_ENTRY` | Regra de domínio inválida, como `PAID` sem `paymentDate` |
| 429 | `RATE_LIMIT_EXCEEDED` | Limite local de requisições excedido |
| 500 | `INTERNAL_ERROR` | Exceção encaminhada ao tratamento genérico |

Erros de parâmetros da URL não possuem um handler específico no projeto e podem cair no tratamento genérico. A documentação não assume que todos os parâmetros ausentes ou malformados retornam 400. As mensagens de validação podem variar conforme o campo e o ambiente.

Fontes dos contratos: [controllers e DTOs de lançamentos](../src/main/java/com/example/cashflow/cashflow/command/api), [consulta](../src/main/java/com/example/cashflow/cashflow/query/api), [diagnósticos](../src/main/java/com/example/cashflow/diagnostics/api) e [tratamento de erros](../src/main/java/com/example/cashflow/shared/exception/GlobalExceptionHandler.java).
