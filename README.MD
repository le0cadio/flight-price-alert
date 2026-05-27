# Flight Price Alert (Kotlin)

Sistema pessoal de monitoramento de preços de voos com alertas por e-mail.

## Objetivo
Monitorar preços de voos internacionais utilizando a API da Amadeus e enviar
alertas automáticos quando o valor estiver abaixo de um limite definido.

Uso pessoal (2 usuários).

## Funcionalidades
- Monitoramento por intervalo de datas ou mês inteiro
- Filtro por companhias aéreas
- Preço alvo configurável
- Verificação periódica (scheduler)
- Envio de alerta por e-mail

## Stack
- Kotlin
- Ktor
- Coroutines
- Amadeus Travel API
- SMTP (Email)
- H2 / SQLite

## Arquitetura
- domain: regras de negócio
- amadeus: integração com API externa
- service: lógica de busca e comparação
- scheduler: execução periódica
- email: envio de notificações
- repository: persistência simples

## Frequência de verificação
Configurável (recomendado: a cada 6 horas)

## Status Atual

O projeto está **funcional e completo para uso pessoal**.
Ele já possui:

- ✅ API HTTP com Ktor
- ✅ CRUD de alertas com validação de entrada
- ✅ Busca de preços por data, faixa e mês
- ✅ Persistência SQL em H2 com criação automática de tabelas
- ✅ Scheduler com start/stop/status
- ✅ Monitoramento automático dos alertas ativos
- ✅ Envio de e-mail via SMTP quando configurado
- ✅ Health check com visão da configuração atual
- ✅ Logs, timeout, retry e rate limit básicos para uso em produção
- ✅ Testes automatizados para repositório, validação, e-mail, serviço e scheduler

### Observação importante

O banco padrão usa arquivo local (`jdbc:h2:file:./data/flightdb`), então os dados ficam salvos entre reinícios.
As tabelas são criadas automaticamente na inicialização.

## Configuração atual

### Variáveis de ambiente

```powershell
$env:APP_ENV = "local" # local | dev | prod
$env:PORT = "8080"
$env:LOG_LEVEL = "DEBUG"
$env:SCHEDULER_INTERVAL_HOURS = "6"
$env:HTTP_TIMEOUT_MS = "10000"
$env:HTTP_RETRY_COUNT = "1"
$env:RATE_LIMIT_PER_MINUTE = "120"
$env:AMADEUS_CLIENT_ID = ""
$env:AMADEUS_CLIENT_SECRET = ""
$env:ALERT_RECIPIENTS = "seu@email.com,outro@email.com"
$env:DB_URL = "jdbc:h2:file:./data/flightdb;AUTO_SERVER=TRUE;MODE=PostgreSQL"
$env:DB_USER = "sa"
$env:DB_PASSWORD = ""
$env:SMTP_HOST = ""
$env:SMTP_PORT = "587"
$env:SMTP_USER = ""
$env:SMTP_PASSWORD = ""
```

### Rodar a aplicação

```powershell
./gradlew.bat run
```

### Rodar os testes

```powershell
./gradlew.bat test
```

## Endpoints

### Básicos

- `GET /`
- `GET /health`
- `GET /config`

### Alertas

- `POST /api/flights/alerts`
- `GET /api/flights/alerts`
- `GET /api/flights/alerts/{id}`
- `PUT /api/flights/alerts/{id}`
- `DELETE /api/flights/alerts/{id}`
- `POST /api/flights/alerts/{id}/toggle`

### Busca de preços

- `GET /api/flights/search?origin=GRU&destination=BKK&departureDate=2026-09-01`
- `GET /api/flights/search?origin=GRU&destination=BKK&month=2026-09`
- `GET /api/flights/search?origin=GRU&destination=BKK&departureDate=2026-09-01&endDate=2026-09-10`
- `GET /api/flights/price-history/{alertId}`

### E-mail e scheduler

- `POST /api/email/test`
- `GET /api/scheduler/status`
- `POST /api/scheduler/start`
- `POST /api/scheduler/stop`

## Arquitetura

- `domain`: modelos e serialização
- `config`: leitura de ambiente e configuração do app
- `repository`: persistência SQL em H2
- `amadeus`: integração com a API externa da Amadeus
- `service`: regra de negócio e monitoramento
- `email`: envio de notificações por e-mail
- `scheduler`: execução periódica do monitoramento
- `api`: rotas HTTP e validação de entrada

## Como funciona

1. Você cria um alerta com origem, destino e preço-alvo.
2. O scheduler roda periodicamente e verifica os alertas ativos.
3. O cliente da Amadeus busca o menor preço disponível.
4. Se o preço estiver abaixo do alvo, a aplicação salva histórico e envia e-mail.
5. O log de notificações evita alertas duplicados quando o preço não melhora.

## Observações

- Projeto com foco em uso pessoal.
- Código preparado para evolução futura.
- Segurança de dados depende de configuração externa e boas práticas no ambiente de execução.
