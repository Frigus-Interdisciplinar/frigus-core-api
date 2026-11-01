# Diagnóstico do contexto e da chamada de IA

## Contratos locais comparados sem publicar alterações

- `HEAD` (`f824262`): o cliente cria `/chats` sem corpo e envia `{content, stock_id}` a `/chats/{id}/messages`, usando `X-API-Key`.
- `origin/dev` (`32c95fd`): usa `/v1/chats` sem corpo e `/v1/chats/{id}/messages` com apenas `{content}`. O estoque solicitado pelo mobile não é transmitido por esse cliente.
- Working tree anterior a este diagnóstico: utiliza `/internal/core/chat`, `X-Core-Service-Key` e `{userId, stockId, message, sessionId, foodPreferences}`. Esse contrato delega a consulta de estoque ao serviço de IA. `availableItems`, embora montado por `AiRecipeChatService`, não faz parte da requisição interna; não foi acrescentado outro payload ou fallback neste diagnóstico.

Esses estados locais não demonstram qual revisão está implantada no servidor. Em particular, testes do working tree não validam um deployment que ainda utilize o cliente legado. O contrato Python versionado atual recebe somente `content` nas mensagens públicas; contexto de estoque/autenticação deve ser conferido contra a revisão efetivamente implantada, sem misturar esses contratos com a rota interna.

## Estoque e erros

O Core lê os lotes do `stockId` solicitado, depois de validar acesso ao grupo, e passa esse mesmo identificador ao cliente interno. O teste de regressão utiliza estoque `1813`, um lote de Suco de frutas com saldo `43` e validade `2028-07-28`: o identificador e os dados preparados não são substituídos por outro estoque. O teste usa repositórios/HTTP simulados; não envia chat real nem grava no serviço externo.

`AiRecipeChatRequestDto.sessionId` é uma String sem restrição UUID/Mongo; `foodPreferences` tem no máximo nove itens. A rota `/ai/recipes/chat` retorna 200 quando válida. Falhas de validação do DTO retornam 400 pelo handler existente, com `displayMessage`. O cliente interno mantém a conversão de HTTP upstream, timeout e falha de decodificação em 503 `SERVICE_UNAVAILABLE`; um 401 de credencial entre serviços não é propagado como logout do usuário mobile.

Os timeouts não foram alterados: conexão Core→IA de 10 segundos, leitura de 110 segundos. Eles devem ser comparados às configurações efetivamente implantadas no BFF e no Android, não apenas ao timeout global do cliente mobile.

## Observabilidade adicionada

Logs informam estoque solicitado, quantidade de lotes, presença de uma conversa anterior, status HTTP upstream, número de receitas retornadas e duração. Falhas conservam classes, frames e encadeamento de causas em uma exceção de diagnóstico sanitizada. Mensagens do provider e corpos de erro não são copiados para os logs; chaves, tokens, texto do usuário e conteúdo da conversa também não são registrados. A resposta pública continua amigável e sem stack trace.

Os testes verificam status upstream 422 identificável sem exposição do corpo/chave, causa `SocketTimeoutException` identificável, encaminhamento do estoque real e os contratos de respostas já existentes. A baseline de IA foi executada antes das alterações: 22 testes passaram. A criação financeira de lotes e seus testes existentes permanecem separados e preservados.

Após as mudanças, `mvn -Dtest=FrigusAiClientTest,AiRecipeChatServiceTest,StockProductServiceTest,StockPurchaseRecorderTest test` passou com 33 testes (24 de IA e os nove financeiros existentes), sem falhas ou erros, usando JDK 23 e release 17. Não foi executado POST real de chat nem um deploy.
