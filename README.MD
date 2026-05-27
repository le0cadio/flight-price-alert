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

## Status Atual (MVP em andamento)

- ✅ Infra básica Ktor pronta (`/` e `/health`)
- ✅ Config centralizada por variáveis de ambiente (`Config.kt`)
- ✅ Persistência SQL em H2 (alertas, histórico e notificações)
- ✅ API de alertas (CRUD + toggle)
- ✅ API de busca de preço (`/api/flights/search`)
- ✅ API de histórico de preço (`/api/flights/price-history/{alertId}`)
- ✅ Scheduler com start/stop/status
- ✅ Simulação de envio de e-mail para fluxo local

### Observação importante

Neste momento o projeto está preparado para **rodar localmente em memória**.
As configurações de banco (DB_URL/DB_USER/DB_PASSWORD) já existem para evolução futura,
mas a implementação ativa usa armazenamento em memória para facilitar desenvolvimento.

## Endpoints já disponíveis

- `GET /health`
- `GET /`
- `GET /config`
- `POST /api/flights/alerts`
- `GET /api/flights/alerts`
- `GET /api/flights/alerts/{id}`
- `PUT /api/flights/alerts/{id}`
- `DELETE /api/flights/alerts/{id}`
- `POST /api/flights/alerts/{id}/toggle`
- `GET /api/flights/search?origin=GRU&destination=BKK&departureDate=2026-09-01`
- `GET /api/flights/price-history/{alertId}`
- `POST /api/email/test`
- `GET /api/scheduler/status`
- `POST /api/scheduler/start`
- `POST /api/scheduler/stop`

## Rodando localmente

Pré-requisito: Java 17+ configurado (`JAVA_HOME` + `java` no PATH).

PowerShell:

```powershell
$env:PORT = "8080"
$env:SCHEDULER_INTERVAL_HOURS = "6"
$env:AMADEUS_CLIENT_ID = ""
$env:AMADEUS_CLIENT_SECRET = ""
$env:ALERT_RECIPIENT = ""
./gradlew.bat run
```

Para rodar testes:

```powershell
./gradlew.bat test
```

## TODO

- 🔧 Infraestrutura

    Configurar ambiente Kotlin + Gradle
    Subir servidor Ktor 
    Criar endpoint de health check 
    Configurar arquivos de configuração  
    Carregar configurações sensíveis via Ktor (email, Amadeus) 

  
- ✈️ Integração com Amadeus API

Criar cliente HTTP para Amadeus

Implementar autenticação OAuth (clientId / clientSecret)

Realizar primeira busca de voos (GRU → BKK)

Suportar busca por intervalo de datas e por mês inteiro

Filtrar resultados por companhias aéreas

Extrair menor preço disponível

- ⏱️ Scheduler

Implementar job periódico (a cada X horas)

Executar busca de preços automaticamente

Evitar notificações duplicadas

Registrar último preço notificado

- 📧 Notificações por E-mail

Configurar SMTP

Criar serviço de envio de e-mail

Criar template simples de e-mail

Enviar alerta quando preço ≤ valor alvo

Suportar múltiplos destinatários

- 🗄️ Persistência

Persistência básica já implementada com H2 em memória

- 🧠 Regras de Negócio

Monitorar múltiplos meses por alerta

Permitir múltiplas companhias por alerta

Reenviar alerta apenas se preço cair mais

Permitir desligar alertas individualmente

- 🖥️ Interface (Futuro)

Criar formulário simples (HTML/JS)

Hospedar front-end no GitHub Pages

Integrar front com backend via API

Autenticação simples (opcional)

- 🚀 Produção

Preparar configuração para produção

Escolher hospedagem (Render / Fly.io / Railway)

Configurar variáveis de ambiente no servidor

Monitoramento básico (logs)

- 🧪 Qualidade

Criar testes básicos de serviço

Criar testes da integração com Amadeus (mock)

Validar comportamento do scheduler

- 📌 Observações

Projeto com foco em uso pessoal

Código preparado para evolução futura (ex: affiliate / referral)

Segurança de dados garantida via .gitignore e config externa