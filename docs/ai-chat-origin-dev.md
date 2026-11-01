# Patch seletivo do chat sobre origin/dev

Base exata: `32c95fd3d9ed8bb12a349137a16a76b0be90f3f0`. O arquivo `ai-chat-origin-dev.patch` foi preparado em checkout detached separado. Ele não foi aplicado ao checkout de trabalho do usuário, não cria commit ou branch e não publica serviço.

## Conteúdo e preservação de NFC-e

O patch altera somente `.env.example`, `FrigusAiClient`, os quatro DTOs de chat, `AiRecipeChatService`, `RecipeSuggestionRepository` e os dois testes de IA. Nenhum arquivo de estoque, compra financeira, migração ou permissão é incluído. O recorder financeiro anterior não faz parte desse artefato e não é duplicado.

A integração de chat usa o contrato interno já existente no Python: `POST /internal/core/chat`, com identidade do Core, estoque escolhido, mensagem, conversa e preferências alimentares. Receitas são associadas pelos IDs explícitos retornados nesse turno. Não utiliza a criação pública legada de chat, que perdia o estoque/usuário do mobile.

`FrigusAiClient.extractInvoice`, todos os records `Invoice*`, `/v1/invoices/extract`, o processamento em lotes de dez produtos e a combinação dos resultados de `origin/dev` foram preservados. A chave pública é aplicada somente nessa requisição. Não publicar a cópia antiga de `FrigusAiClient` que existe no checkout baseado em `f824262`: ela não contém a extração de NFC-e.

## Configuração necessária

- `FRIGUS_AI_BASE_URL`: origem HTTP(S) ou configuração legada terminada em `/v1` ou `/v1/`. O cliente normaliza uma vez para a origem. Chat usa `/internal/core/chat`; NFC-e continua em `/v1/invoices/extract`. Outros prefixos, credenciais na URL, query e fragmento são rejeitados com erro que identifica a variável, sem mostrar seu valor.
- `FRIGUS_AI_API_KEY`: chave pública existente de NFC-e, enviada em `X-API-Key` somente na extração. `FRIGUS_AI_API_KEY_REQUIRED` conserva a validação da chave pública.
- `FRIGUS_AI_CORE_SERVICE_KEY`: chave interna dedicada, com pelo menos 32 caracteres, igual à `API_KEYS__CORE_SERVICE_KEY` do Python. É enviada em `X-Core-Service-Key` somente no chat interno. Ausência dessa chave bloqueia o chat com erro amigável; não desativa a extração de NFC-e e não reutiliza silenciosamente a chave pública.

Não há novo endpoint público. A rota interna precisa existir na revisão Python implantada; aplicar apenas o Core contra um Python antigo não estabelece esse contrato. Conversas criadas pela API pública legada podem não pertencer à identidade interna do Core; nesse caso, iniciar outra conversa explicitamente, sem reenviar automaticamente a mensagem anterior.

## Verificações do artefato

`git apply --check` passou em um segundo checkout limpo e detached da base exata acima. `git apply --reverse --check` passou no checkout com o patch preparado. O checkout limpo permaneceu sem alterações. `git diff --check` também passou.

O build focal é executado no checkout preparado com JDK 23 e release 17:

```powershell
mvn -Dtest=FrigusAiClientTest,AiRecipeChatServiceTest,ReceiptServiceTest test
```

Os testes incluem chat interno, contexto correto de estoque, erros sanitizados, normalização de origem e `/v1`, separação das duas credenciais, extração NFC-e mesmo sem chave de chat e batching de onze produtos em requisições de dez e um. Em 01/11/2026, o build focal passou: 38 testes, nenhuma falha, erro ou teste ignorado; 23 de `FrigusAiClientTest`, 12 de `AiRecipeChatServiceTest` e 3 de `ReceiptServiceTest`, em 1 min 08 s. Todas as 305 fontes de produção e 61 fontes de teste de `origin/dev` foram compiladas. A primeira tentativa identificou uma assinatura de asserção incompatível com a versão de AssertJ da base; ela foi corrigida apenas no checkout preparado antes da execução final. Nenhum POST real de chat ou nota é realizado pelos testes.

Checkouts temporários preparados para inspeção:

- `%TEMP%/frigus-core-chat-ready-20261101`: código com o patch, detached na base.
- `%TEMP%/frigus-core-chat-check-20261101`: base limpa utilizada para verificar aplicação, sem alterar fontes.

Antes de integrar em outra revisão, conferir novamente com `git apply --check` no checkout de destino. Não aplicar esse patch diretamente sobre o WIP antigo do usuário nem substituir arquivos inteiros de uma branch mais nova.
