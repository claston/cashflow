# Guia de execução e demonstração

[Início](../README.md) · [Visão da solução](solucao_fluxo_caixa_consolidada.md) · [Arquitetura da POC](arquitetura_poc.md) · [API](api.md)

Este guia descreve como iniciar a aplicação e observar os fluxos da POC. Os exemplos usam junho de 2026 como massa de demonstração; esse mês não precisa corresponder à data atual.

## Conteúdo

- [Preparação e execução](#preparacao-e-execucao)
- [Configuração local](#configuracao-local)
- [Console H2](#console-h2)
- [Demonstração](#demonstracao)
- [Falha e retomada da projeção](#falha-e-retomada)
- [Demonstração do rate limit](#demonstracao-do-rate-limit)
- [Testes existentes](#testes-existentes)
- [Solução de problemas](#solucao-de-problemas)

<a id="preparacao-e-execucao"></a>

## Preparação e execução

É necessário ter JDK 21 e Maven no `PATH`. Para os exemplos, use PowerShell com `Invoke-RestMethod` ou Bash com `curl`. A primeira resolução das dependências Maven requer acesso aos repositórios configurados. A aplicação usa a porta 8080 por padrão e não exige Docker ou serviços de nuvem.

Execute na raiz do repositório:

```text
java -version
mvn -version
mvn spring-boot:run
```

Confira no resultado de `mvn -version` se o Maven também está usando Java 21. O projeto não contém `mvnw` ou `mvnw.cmd`.

Mantenha esse terminal aberto para acompanhar os logs. Em outro terminal, verifique a saúde da aplicação:

**Bash:**

```bash
curl -sS http://localhost:8080/actuator/health
```

**PowerShell:**

```powershell
Invoke-RestMethod -Uri 'http://localhost:8080/actuator/health'
```

O resultado esperado contém `status: UP`. Esse estado indica saúde básica; não verifica se o resumo mensal já foi atualizado.

Para encerrar, use `Ctrl+C` no terminal da aplicação. Lançamentos e outbox persistem em `data/`; os componentes em memória são reiniciados na próxima execução. Iniciar a aplicação em outro diretório pode apontar para outro arquivo H2, pois o caminho do banco é relativo.

<a id="configuracao-local"></a>

## Configuração local

Os valores abaixo estão em [application.properties](../src/main/resources/application.properties).

| Propriedade | Padrão | Efeito |
| --- | --- | --- |
| `spring.datasource.url` | `jdbc:h2:file:./data/cashflow;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE` | Banco persistido em arquivo |
| `spring.datasource.username` | `sa` | Usuário local |
| `spring.datasource.password` | Vazia | Senha local |
| `spring.jpa.hibernate.ddl-auto` | `update` | Atualização do esquema pelo Hibernate |
| `spring.h2.console.enabled` | `true` | Habilita o console H2 |
| `spring.h2.console.path` | `/h2-console` | Caminho do console |
| `spring.task.scheduling.pool.size` | `4` | Pool das tarefas agendadas |
| `management.endpoints.web.exposure.include` | `health,info` | Endpoints Actuator expostos |
| `app.outbox.publisher-delay-ms` | `2000` | Espera após cada ciclo do publicador, em ms |
| `app.outbox.batch-size` | `10` | Máximo de eventos buscados por ciclo |
| `app.outbox.max-attempts` | `3` | Limite de falhas de publicação antes de `FAILED` |
| `app.projection.worker-delay-ms` | `1000` | Espera após cada ciclo do worker, em ms |
| `app.projection.max-events-per-cycle` | `20` | Máximo de eventos consumidos por ciclo |
| `app.rate-limit.enabled` | `true` | Ativa o filtro de limite de requisições |
| `app.rate-limit.requests-per-minute` | `60` | Requisições por janela local de 60 segundos e cliente |

Para executar em outra porta sem editar arquivos, em Bash ou PowerShell:

```text
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

Nesse caso, ajuste a porta de todas as URLs dos exemplos.

<a id="console-h2"></a>

## Console H2

Com a aplicação em execução, abra `http://localhost:8080/h2-console` e use:

| Campo | Valor |
| --- | --- |
| Driver Class | `org.h2.Driver` |
| JDBC URL | `jdbc:h2:file:./data/cashflow;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE` |
| User Name | `sa` |
| Password | Deixe vazio |

Para inspecionar os lançamentos e o andamento das publicações:

```sql
SELECT id, company_id, type, amount, due_date, status
FROM cashflow_entries
ORDER BY created_at DESC;

SELECT id, aggregate_id, event_type, status, attempts, last_error
FROM outbox_events
ORDER BY created_at DESC;
```

O `id` da outbox é diferente do `eventId` dentro do payload do evento. Relacione o lançamento pelo `aggregate_id`.

<a id="demonstracao"></a>

## Demonstração

### 1. Preparar uma empresa para a demonstração

Use uma empresa sem lançamentos anteriores. O exemplo PowerShell gera um UUID; no exemplo Bash, substitua o UUID de exemplo se ele já foi utilizado. Não é necessário cadastrar a empresa.

Escolha o bloco do seu terminal e mantenha as variáveis na mesma sessão durante o roteiro. Cada POST cria um novo lançamento; executar o mesmo bloco duas vezes altera os totais.

### 2. Criar uma receita paga e uma despesa pendente

**Bash:**

```bash
base_url='http://localhost:8080'
company_id='11111111-1111-1111-1111-111111111111'
client_id='docs-demo'

curl -fsS -X POST "$base_url/api/cashflow-entries" \
  -H 'Content-Type: application/json' \
  -H "X-Client-Id: $client_id" \
  -H 'X-Correlation-Id: docs-demo-income' \
  --data-binary @- <<JSON
{
  "companyId": "$company_id",
  "description": "Venda de servico",
  "type": "INCOME",
  "amount": 1500.00,
  "category": "SALES",
  "account": "MAIN_ACCOUNT",
  "dueDate": "2026-06-10",
  "paymentDate": "2026-06-10",
  "status": "PAID"
}
JSON

curl -fsS -X POST "$base_url/api/cashflow-entries" \
  -H 'Content-Type: application/json' \
  -H "X-Client-Id: $client_id" \
  --data-binary @- <<JSON
{
  "companyId": "$company_id",
  "description": "Compra de material",
  "type": "EXPENSE",
  "amount": 300.00,
  "category": "SUPPLIES",
  "account": "MAIN_ACCOUNT",
  "dueDate": "2026-06-15",
  "paymentDate": null,
  "status": "PENDING"
}
JSON
```

**PowerShell:**

```powershell
$baseUrl = 'http://localhost:8080'
$demoCompanyId = [guid]::NewGuid().ToString()
$demoHeaders = @{ 'X-Client-Id' = 'docs-demo' }
$demoEntries = @(
    @{
        companyId = $demoCompanyId
        description = 'Venda de servico'
        type = 'INCOME'
        amount = 1500.00
        category = 'SALES'
        account = 'MAIN_ACCOUNT'
        dueDate = '2026-06-10'
        paymentDate = '2026-06-10'
        status = 'PAID'
    },
    @{
        companyId = $demoCompanyId
        description = 'Compra de material'
        type = 'EXPENSE'
        amount = 300.00
        category = 'SUPPLIES'
        account = 'MAIN_ACCOUNT'
        dueDate = '2026-06-15'
        paymentDate = $null
        status = 'PENDING'
    }
)

foreach ($demoEntry in $demoEntries) {
    $requestHeaders = $demoHeaders.Clone()
    $requestHeaders['X-Correlation-Id'] = [guid]::NewGuid().ToString()
    Invoke-RestMethod -Method Post -Uri "$baseUrl/api/cashflow-entries" `
        -Headers $requestHeaders -ContentType 'application/json' `
        -Body ($demoEntry | ConvertTo-Json)
}

$demoCompanyId
```

Cada resposta deve conter um lançamento com `id` e `createdAt` gerados. A criação retorna HTTP 200. Nesse momento, a projeção pode ainda estar pendente.

### 3. Acompanhar publicação e projeção

**Bash:**

```bash
curl -fsS -H "X-Client-Id: $client_id" "$base_url/api/diagnostics/outbox"
curl -fsS -H "X-Client-Id: $client_id" "$base_url/api/diagnostics/read-model"
```

**PowerShell:**

```powershell
Invoke-RestMethod -Headers $demoHeaders -Uri "$baseUrl/api/diagnostics/outbox"
$demoReadModel = Invoke-RestMethod -Headers $demoHeaders `
    -Uri "$baseUrl/api/diagnostics/read-model"
$demoReadModel."${demoCompanyId}:2026-06"
```

Aguarde os ciclos do publicador e do worker e repita a inspeção até o resumo da empresa mostrar **`entriesCount: 2`**, receita de 1.500 e despesa de 300. O diagnóstico da outbox é global e pode incluir outras demonstrações.

Use o endpoint de diagnóstico para essa espera. Consultar o resumo mensal antes da projeção pode acionar o fallback e depois receber os mesmos valores novamente pelo worker. `PUBLISHED`, isoladamente, não confirma que a projeção terminou.

### 4. Consultar os lançamentos e o resumo

**Bash:**

```bash
curl -fsS -H "X-Client-Id: $client_id" \
  "$base_url/api/cashflow-entries?companyId=$company_id&month=2026-06"
curl -fsS -H "X-Client-Id: $client_id" \
  "$base_url/api/cashflow-summary/monthly?companyId=$company_id&month=2026-06"
```

**PowerShell:**

```powershell
Invoke-RestMethod -Headers $demoHeaders `
    -Uri "$baseUrl/api/cashflow-entries?companyId=$demoCompanyId&month=2026-06"
Invoke-RestMethod -Headers $demoHeaders `
    -Uri "$baseUrl/api/cashflow-summary/monthly?companyId=$demoCompanyId&month=2026-06"
```

Valores esperados para os dois lançamentos, após a projeção:

| Campo | Esperado |
| --- | --- |
| `totalIncome` | 1500 |
| `totalExpense` | 300 |
| `projectedBalance` | 1200 |
| `paidIncome` | 1500 |
| `paidExpense` | 0 |
| `realizedBalance` | 1500 |
| `entriesCount` | 2 |
| `source` | `READ_MODEL` |

### 5. Demonstrar o fallback

Após confirmar os dois eventos projetados, mantenha a aplicação sem novas escritas. O comando abaixo limpa os resumos **de todas as empresas**, preservando os lançamentos e a outbox.

**Bash:**

```bash
curl -fsS -X POST -H "X-Client-Id: $client_id" \
  "$base_url/api/diagnostics/read-model/clear"
curl -fsS -H "X-Client-Id: $client_id" \
  "$base_url/api/cashflow-summary/monthly?companyId=$company_id&month=2026-06"
```

**PowerShell:**

```powershell
Invoke-RestMethod -Method Post -Headers $demoHeaders `
    -Uri "$baseUrl/api/diagnostics/read-model/clear"
Invoke-RestMethod -Headers $demoHeaders `
    -Uri "$baseUrl/api/cashflow-summary/monthly?companyId=$demoCompanyId&month=2026-06"
```

A primeira consulta deve retornar os mesmos totais com `source: FALLBACK_RECALCULATION`. Repetindo apenas a consulta, o esperado é `source: READ_MODEL`.

<a id="falha-e-retomada"></a>

## Falha e retomada da projeção

Use a mesma sessão do roteiro anterior. Para ativar o modo de falha:

**Bash:**

```bash
curl -fsS -X POST "$base_url/api/diagnostics/projection/failure-mode" \
  -H 'Content-Type: application/json' -H "X-Client-Id: $client_id" \
  -d '{"enabled": true}'
```

**PowerShell:**

```powershell
Invoke-RestMethod -Method Post -Headers $demoHeaders `
    -Uri "$baseUrl/api/diagnostics/projection/failure-mode" `
    -ContentType 'application/json' -Body '{"enabled": true}'
```

Crie **mais uma despesa pendente de 100**, usando o formato do passo 2 e a mesma empresa e mês. Observe nos logs a mensagem `Projection failure mode is enabled.` e confira a projeção pelo diagnóstico, sem limpar o modelo ou acionar fallback durante a simulação.

Para retomar, repita o comando de configuração substituindo `true` por `false`. Aguarde o worker e confira pelo diagnóstico que o resumo contém três lançamentos: `totalExpense: 400`, `projectedBalance: 1100` e `realizedBalance: 1500`.

A falha simulada devolve o evento à fila em memória. Ela não gera um `FAILED` na outbox, por isso não exige `reprocess-failed`. Esse endpoint se aplica a falhas do publicador. Não reinicie a aplicação durante esse cenário, pois a fila é volátil.

<a id="demonstracao-do-rate-limit"></a>

## Demonstração do rate limit

Com a configuração padrão, envie 61 chamadas em menos de 60 segundos usando uma chave nova. O exemplo usa diagnósticos para não criar lançamentos.

**Bash:**

```bash
rate_client_id="docs-rate-$(date +%s)-$RANDOM"
for attempt in {1..61}; do
  curl -sS -o /dev/null -w '%{http_code}\n' \
    -H "X-Client-Id: $rate_client_id" \
    "$base_url/api/diagnostics/outbox"
done
```

**PowerShell:**

```powershell
$rateHeaders = @{ 'X-Client-Id' = "docs-rate-$([guid]::NewGuid())" }
1..61 | ForEach-Object {
    try {
        $null = Invoke-RestMethod -Headers $rateHeaders `
            -Uri "$baseUrl/api/diagnostics/outbox"
        200
    } catch {
        if ($null -ne $_.Exception.Response) {
            [int]$_.Exception.Response.StatusCode
        } else {
            throw
        }
    }
}
```

São esperadas 60 respostas 200 e, dentro da mesma janela, uma resposta 429 com `RATE_LIMIT_EXCEEDED`. Depois de 60 segundos desde o início da janela, uma nova chamada pode ser aceita. O resultado depende de não haver outras chamadas com a mesma chave e de o laço terminar dentro da janela.

<a id="testes-existentes"></a>

## Testes existentes

Com a aplicação encerrada, execute na raiz:

```text
mvn test
```

Encerrar a aplicação evita disputa pelo arquivo H2 quando um teste carrega o contexto Spring. Os arquivos em [src/test](../src/test) incluem cenários de criação e validação de lançamentos, cálculo do resumo, fallback, rate limit e correlação HTTP.

Os resultados da execução ficam em `target/surefire-reports`. A existência desses testes não representa comprovação das metas de carga ou disponibilidade da solução. Esta revisão documental não executou nem alterou a suíte.

<a id="solucao-de-problemas"></a>

## Solução de problemas

| Sintoma | O que conferir |
| --- | --- |
| `mvn` ou `java` não encontrado | Instalação, `PATH` e `JAVA_HOME`; compare `java -version` com `mvn -version` |
| Java incompatível | O `pom.xml` está configurado para Java 21 |
| Falha ao resolver dependências | Conectividade e configuração dos repositórios/proxy do Maven |
| Porta 8080 ocupada | Outra instância em execução; encerre-a ou utilize a opção de porta descrita acima |
| Banco bloqueado | Outro processo usando o mesmo arquivo H2, inclusive outra aplicação ou teste |
| Console H2 mostra banco vazio | JDBC URL, opções da conexão e diretório de trabalho da aplicação |
| Resumo ainda não apareceu | Aguarde os ciclos, confira outbox, logs e modo de falha; use o diagnóstico do read model |
| Resumo com valores duplicados ou parciais | Confira os registros oficiais. Após cessar escritas e confirmar o processamento dos eventos, limpe a projeção e consulte novamente; veja os limites na arquitetura |
| HTTP 429 inesperado | Reutilização da mesma chave, inclusive `anonymous`; aguarde a janela e confira o cabeçalho |
| Totais diferentes da tabela do roteiro | UUID já usado, POST repetido, consulta antes da projeção ou outros lançamentos no mesmo mês |
| Eventos publicados sem resumo após reinício | Fila e projeção foram perdidas; `PUBLISHED` não dispara replay automático. Um resumo ausente pode ser recalculado na consulta |

As explicações detalhadas sobre recuperação, concorrência entre fallback e projeção e persistência estão nas [garantias e limitações](arquitetura_poc.md#garantias-e-limitacoes).
