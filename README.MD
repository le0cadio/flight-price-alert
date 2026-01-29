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

## Próximos passos
1. Criar projeto Kotlin no IntelliJ
2. Configurar Ktor
3. Integrar Amadeus API
4. Implementar scheduler
5. Implementar envio de e-mail
