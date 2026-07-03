# Flight Price Alert (Kotlin)

Sistema pessoal de monitoramento de preços de voos com alertas por e-mail.

## Objetivo

Monitorar preços de voos internacionais usando a API da Amadeus e enviar um
e-mail quando o preço estiver com um bom valor — abaixo da média histórica da
rota, ou no menor preço já visto — pra você conseguir decidir na hora certa
se compra ou espera mais um pouco.

Uso pessoal (2 usuários).

## Como funciona a recomendação

O sistema não dispara e-mail só por bater um preço-alvo fixo. Depois de
acumular pelo menos 3 checagens de histórico para uma rota, cada nova
checagem é comparada com a média e o menor preço já vistos:

- 🟢 **Compre agora** — é o menor preço já registrado para essa rota.
- 🟢 **Bom momento pra comprar** — o preço está bem abaixo da média histórica
  (10% por padrão, configurável).
- 🟡 **Pode valer esperar um pouco** — está abaixo da média e ainda caindo,
  mas não o suficiente pra ser considerado "bom preço" ainda.
- Nenhum e-mail é enviado fora desses casos, nem enquanto o histórico ainda
  está sendo construído (menos de 3 checagens).

Você também pode opcionalmente definir um `targetPrice` como teto: acima
dele, nunca notifica, não importa a recomendação.

## Arquitetura 100% gratuita

| Peça | Solução |
|---|---|
| Verificação periódica | GitHub Actions (cron a cada 6h) rodando `./gradlew runChecks` |
| Banco de dados | Supabase (Postgres free tier) |
| E-mail | Gmail SMTP com "senha de app" |
| API pra gerenciar alertas (opcional) | Render free tier (Docker) |
| Preço de voo | API da Amadeus (self-service) |

O `RunChecks.kt` é um entrypoint separado do servidor HTTP: ele só carrega a
config, checa todos os alertas ativos uma vez e sai. É isso que o GitHub
Actions executa periodicamente — não precisa de nenhum servidor ligado 24/7.

## Stack

- Kotlin, Ktor, Coroutines
- Amadeus Travel API
- SMTP (Email)
- H2 (local/testes) ou Postgres/Supabase (produção)

## Passo a passo pra colocar no ar

### 1. Supabase (banco de dados)

1. Crie uma conta em [supabase.com](https://supabase.com) (sem cartão) e um
   novo projeto.
2. Em **Project Settings → Database**, copie a connection string no modo
   *Session pooler* (funciona melhor com conexões curtas como as do GitHub
   Actions). Algo como:
   `postgresql://postgres.xxxxx:[SENHA]@aws-0-region.pooler.supabase.com:5432/postgres`
3. Vamos usar essa informação nos Secrets do GitHub (`DB_URL`, `DB_USER`,
   `DB_PASSWORD`) no passo 4. Formato do `DB_URL` pro nosso driver JDBC:
   `jdbc:postgresql://aws-0-region.pooler.supabase.com:5432/postgres?sslmode=require`

### 2. Gmail (envio de e-mail)

1. Ative a verificação em duas etapas na sua conta Google.
2. Gere uma "senha de app" em
   [myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords).
3. Guarde: `SMTP_HOST=smtp.gmail.com`, `SMTP_PORT=587`, `SMTP_USER=<seu gmail>`,
   `SMTP_PASSWORD=<senha de app gerada>`.

### 3. Amadeus for Developers (preço de voo)

1. Crie uma conta em [developers.amadeus.com](https://developers.amadeus.com)
   e um app self-service — isso já te dá `AMADEUS_CLIENT_ID` e
   `AMADEUS_CLIENT_SECRET` no ambiente de **teste**, sem custo.
2. Quando quiser preços reais (recomendado antes de decidir uma compra),
   solicite acesso de **produção** no mesmo app — não pede cartão, e você
   mantém a cota mensal gratuita também em produção (paga só o que passar
   dela). Depois é só trocar:
   `AMADEUS_BASE_URL=https://api.amadeus.com`
3. `AMADEUS_MAX_DATES_PER_CHECK` (padrão 10) limita quantas datas dentro de
   uma janela larga (ex: o mês inteiro) são consultadas por checagem, pra não
   estourar a cota gratuita quando o alerta cobre muitos dias.

### 4. Secrets no GitHub

No seu repositório: **Settings → Secrets and variables → Actions → New
repository secret**. Cadastre (nenhum desses vai pro código, ficam só aqui):

`AMADEUS_CLIENT_ID`, `AMADEUS_CLIENT_SECRET`, `AMADEUS_BASE_URL`,
`AMADEUS_MAX_DATES_PER_CHECK`, `DB_URL`, `DB_USER`, `DB_PASSWORD`,
`SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`, `ALERT_RECIPIENTS`
(e-mails separados por vírgula).

### 5. Ativar o workflow

O workflow já está em `.github/workflows/check-prices.yml`, rodando a cada 6h
(`workflow_dispatch` também permite rodar manualmente pela aba **Actions** do
GitHub pra testar sem esperar o cron). Assim que os Secrets estiverem
cadastrados, ele já funciona sozinho.

### 6. Criar seu primeiro alerta

A tabela de alertas é criada automaticamente na primeira execução. Pra
cadastrar um alerta você precisa da API HTTP rodando em algum lugar (veja o
passo 7) ou pode rodar localmente:

```bash
cp .env.example .env   # preencha com Supabase/Amadeus/SMTP
./gradlew run
```

Depois:

```bash
curl -X POST http://localhost:8080/api/flights/alerts \
  -H "Content-Type: application/json" \
  -d '{
    "origin": "GRU",
    "destination": "BKK",
    "departureDateFrom": "2026-11-01",
    "departureDateTo": "2026-11-30",
    "targetPrice": 4500
  }'
```

`targetPrice` é opcional — é só um teto de segurança; a recomendação
principal já é baseada na média histórica.

### 7. (Opcional) Deploy da API no Render

Isso é só pra ter uma forma cômoda de cadastrar/editar alertas remotamente —
os e-mails continuam sendo disparados pelo GitHub Actions independente disso.

1. No Render, **New → Web Service → Build and deploy from a Dockerfile**
   apontando pro seu repositório.
2. Configure as mesmas variáveis de ambiente do passo 4 (Render não lê
   Secrets do GitHub, precisa cadastrar de novo lá).
3. Deixe `ENABLE_INTERNAL_SCHEDULER=false` (padrão) — assim essa instância
   não duplica os e-mails que o GitHub Actions já envia; ela serve só pra
   gerenciar alertas via HTTP.
4. O plano free do Render "dorme" depois de um tempo sem uso — sem problema
   aqui, já que você só acessa pra cadastrar/editar alertas de vez em quando.

## Variáveis de ambiente

Veja `.env.example` para a lista completa com comentários.

## Endpoints

### Básicos

- `GET /`
- `GET /health`
- `GET /config`

### Alertas

- `POST /api/flights/alerts` — `origin`, `destination`, `departureDateFrom`,
  `departureDateTo` (opcional), `targetPrice` (opcional), `airlines` (opcional)
- `GET /api/flights/alerts`
- `GET /api/flights/alerts/{id}`
- `PUT /api/flights/alerts/{id}`
- `DELETE /api/flights/alerts/{id}`
- `POST /api/flights/alerts/{id}/toggle`

### Busca de preços (ad-hoc, fora do fluxo de alertas)

- `GET /api/flights/search?origin=GRU&destination=BKK&departureDate=2026-11-01`
- `GET /api/flights/search?origin=GRU&destination=BKK&month=2026-11`
- `GET /api/flights/search?origin=GRU&destination=BKK&departureDate=2026-11-01&endDate=2026-11-10`
- `GET /api/flights/price-history/{alertId}`

### E-mail e scheduler interno (só relevante rodando localmente)

- `POST /api/email/test`
- `GET /api/scheduler/status`
- `POST /api/scheduler/start`
- `POST /api/scheduler/stop`

## Arquitetura

- `domain`: modelos, serialização e regras de recomendação (`Recommendation`)
- `config`: leitura de ambiente e configuração do app
- `repository`: persistência SQL (H2 local / Postgres em produção)
- `amadeus`: integração com a API externa da Amadeus
- `service`: regra de negócio, monitoramento e motor de recomendação
- `email`: envio de notificações por e-mail
- `scheduler`: execução periódica (usada só se `ENABLE_INTERNAL_SCHEDULER=true`)
- `api`: rotas HTTP e validação de entrada

## Rodar localmente

```bash
cp .env.example .env
./gradlew run     # sobe a API HTTP (usa H2 local por padrão)
./gradlew test    # roda a suíte de testes
./gradlew runChecks  # roda uma checagem única de todos os alertas ativos (o que o GH Actions executa)
```

## Segurança

- Nenhuma credencial fica no código ou é commitada — só em `.env` (local,
  ignorado pelo git), Secrets do GitHub Actions e variáveis de ambiente no
  Render.
- O preço nunca é inventado: se a Amadeus não conseguir retornar um preço, a
  checagem daquele ciclo é simplesmente pulada (sem gravar histórico, sem
  notificar).
