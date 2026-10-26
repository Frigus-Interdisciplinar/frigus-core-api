# Plano de correções de segurança — Frigus Core API

**Projeto:** `C:\Users\gustavopm-ieg\Frigus`  
**Referência:** F2O-163 — `[Back] Implementar recursos de segurança na API`  
**Base da análise:** código local examinado em 2026-10-07 e [workitem F2O-163](https://frigus-inter-2o.atlassian.net/browse/F2O-163) consultado em 2026-10-08. A descrição menciona rate limit, CORS e proteção da API key; não há subtarefas nem comentários com critérios adicionais.  
**Natureza:** plano de implementação. Nenhuma correção foi aplicada nesta análise.

## Ordem e escopo

As etapas 1–3 fecham acessos entre usuários/grupos; as etapas 4–5 tratam transporte e navegador; as etapas 6–7 limitam abuso; a etapa 8 cobre a API key citada na F2O-163; a etapa 9 valida o conjunto. **F2O-163** identifica apenas os temas confirmados na descrição consultada: rate limit, CORS e proteção da API key. As demais etapas são achados adicionais da auditoria.

### Etapa 1 — Fechar o acesso entre grupos em listas de compras

- **Objetivo:** impedir leitura e alteração de listas e itens de outros grupos.
- **Achado/evidência:** A1 e A2. `ShoppingListService.findAll` usa `repository.findAll(pageable)` quando `stockId` e `groupId` não são informados (`src/main/java/com/frigus/coreapi/service/ShoppingListService.java:97–125`). `ShoppingListProductService` carrega lista e item por ID sem verificar acesso ao grupo (`.../service/ShoppingListProductService.java:44–151`); as rotas estão em `.../controller/ShoppingListController.java:43–141`.
- **Escopo:** adicional.
- **Arquivos e locais:** `ShoppingListService.findAll`; todos os métodos públicos de `ShoppingListProductService`; `ShoppingListRepository` e `ShoppingListProductRepository`, se forem necessárias consultas escopadas.
- **Ações a executar:**
  1. Fazer a consulta sem filtros retornar somente listas de grupos acessíveis ao usuário. Preservar os filtros por estoque, grupo e status.
  2. Centralizar a obtenção de lista autorizada e chamá-la antes de ler ou alterar seus itens, inclusive no batch.
  3. Verificar que `itemId` pertence ao `listId` solicitado e ao grupo autorizado.
- **Testes e validação:** criar cenários HTTP com dois usuários de grupos distintos; testar listagem sem filtro, com filtro e cada operação de item; confirmar que pedidos negados não gravam dados. Executar `./mvnw clean verify -B` (Linux/CI) ou `.\mvnw.cmd clean verify -B` (Windows).
- **Dependências ou decisões pendentes:** preservar a exceção de acesso administrativo definida em `GroupAccessService`.
- **Critério de conclusão:** usuário comum não obtém nem modifica lista ou item fora dos próprios grupos.

### Etapa 2 — Autorizar inscrições WebSocket e vínculos de mensagens

- **Objetivo:** impedir leitura de tópicos privados e associação de itens de outro grupo a mensagens.
- **Achado/evidência:** A4 e A11. `WebSocketAuthInterceptor.preSend` verifica somente `CONNECT` e sempre devolve a mensagem (`.../security/WebSocketAuthInterceptor.java:29–54`). O broker aceita `/topic` (`.../config/WebSocketConfig.java:20–23`); mensagens são publicadas em tópicos de conversa/grupo (`.../service/MessageService.java:110–125`). `MessageService.sendMessage` carrega `relatedShoppingListProductId` sem verificar seu grupo/lista (`MessageService.java:81–98`).
- **Escopo:** adicional.
- **Arquivos e locais:** `WebSocketAuthInterceptor`, `WebSocketConfig`, `MessageService.sendMessage` e testes de STOMP.
- **Ações a executar:**
  1. Recusar `CONNECT` sem identidade válida e `SUBSCRIBE` fora dos destinos permitidos.
  2. Para tópicos de conversa e grupo, verificar participação/membro atual em cada inscrição; recusar destinos de broker não previstos.
  3. Validar que o item relacionado pertence ao contexto da conversa e é acessível ao remetente.
- **Testes e validação:** cobrir cliente anônimo, membro, externo e ex-membro em `CONNECT`, `SUBSCRIBE` e envio; confirmar que só membros recebem conteúdo. Testar vínculo com item de grupo alheio.
- **Dependências ou decisões pendentes:** confirmar se há algum tópico público legítimo; nenhum foi identificado na análise.
- **Critério de conclusão:** somente destinatários autorizados recebem mensagens, e mensagens só referenciam itens autorizados.

### Etapa 3 — Escopar descartes ao estoque do grupo

- **Objetivo:** proteger listagem, leitura, criação e exclusão de descartes.
- **Achado/evidência:** A3. `DiscardService.create` reduz o saldo de um `StockProduct` buscado por ID sem verificar o grupo (`.../service/DiscardService.java:30–45`). `BaseService` fornece listagem, leitura e exclusão herdadas com autorização vazia por padrão (`.../service/BaseService.java:22–62`); `DiscardController` expõe essas operações (`.../controller/DiscardController.java:20–37`).
- **Escopo:** adicional.
- **Arquivos e locais:** `DiscardService`, `DiscardRepository` e, se necessário, `DiscardController`.
- **Ações a executar:**
  1. Derivar o grupo do produto de estoque ou descarte e aplicar `GroupAccessService` antes de cada operação.
  2. Substituir a listagem global por consulta escopada e paginada.
  3. Garantir que requisição negada não reduza quantidade nem exclua registro.
- **Testes e validação:** testar usuários de grupos distintos com IDs conhecidos; conferir status e saldo antes/depois. Executar a suíte Maven.
- **Dependências ou decisões pendentes:** nenhuma decisão de produto identificada.
- **Critério de conclusão:** apenas membro autorizado ou ADMIN acessa descartes do grupo.

### Etapa 4 — Proteger transporte público e cookies

- **Objetivo:** assegurar TLS no acesso público e transmissão segura dos tokens.
- **Achado/evidência:** A5. O Ingress versionado é público e anuncia somente HTTP:80 (`deploy/k8s/ingress.yaml:7–16`). `AuthController` cria e remove cookies sem atributo `Secure` (`.../controller/AuthController.java:69–104`). O estado real da implantação não foi verificado.
- **Escopo:** adicional.
- **Arquivos e locais:** `deploy/k8s/ingress.yaml`, configuração de domínio/certificado e `AuthController.setTokenCookies/clearTokenCookies`.
- **Ações a executar:**
  1. Confirmar a topologia implantada e configurar HTTPS no ponto público, com redirecionamento de HTTP.
  2. Marcar cookies de acesso e refresh como `Secure`, inclusive os cookies de remoção.
  3. Conferir proxy, terminação TLS e URLs do ambiente de produção.
- **Testes e validação:** validar o manifesto e consultar homologação para conferir certificado, redirecionamento e atributos `Set-Cookie`, sem usar tokens reais na evidência.
- **Dependências ou decisões pendentes:** domínio e certificado de cada ambiente.
- **Critério de conclusão:** acesso público protegido por HTTPS, HTTP redirecionado e cookies seguros.

### Etapa 5 — Restringir CORS e decidir proteção CSRF

- **Objetivo:** permitir somente origens confiáveis e proteger operações autenticadas por cookie.
- **Achado/evidência:** A6 e A7. `SecurityConfig.corsConfigurationSource` aceita qualquer padrão de origem e header com credenciais habilitadas (`.../security/SecurityConfig.java:46–55`); WebSocket também aceita todas as origens (`.../config/WebSocketConfig.java:27–33`). CSRF está desabilitado (`SecurityConfig.java:20`) enquanto `SecurityFilter` aceita cookie `accessToken` (`.../security/SecurityFilter.java:61–75`).
- **Escopo:** CORS atende à **F2O-163**; CSRF é adicional.
- **Arquivos e locais:** `SecurityConfig.filterChain/corsConfigurationSource`, `WebSocketConfig`, configuração por ambiente e testes de segurança.
- **Ações a executar:**
  1. Registrar as origens reais do frontend por ambiente e substituí-las por allowlist explícita para HTTP e WebSocket; limitar métodos e headers ao necessário.
  2. Decidir o contrato de autenticação: se cookies continuarem aceitos, proteger métodos mutáveis, inclusive refresh/logout, contra CSRF; se o contrato passar a Bearer-only, coordenar a retirada dos cookies com o cliente.
  3. Testar preflight e chamadas reais com origem permitida/negada, com e sem credenciais.
- **Testes e validação:** verificar `Access-Control-Allow-Origin`, preflight, origem negada e rejeição de mutação sem proteção CSRF conforme o contrato escolhido.
- **Dependências ou decisões pendentes:** origens por ambiente, contrato do frontend e Etapa 4.
- **Critério de conclusão:** somente origens aprovadas recebem respostas credenciadas e mutações por cookie obedecem à proteção definida.

### Etapa 6 — Limitar o processamento de cliques informados pelo cliente

- **Objetivo:** impedir custo proporcional a um número arbitrário fornecido na requisição.
- **Achado/evidência:** A9. O DTO exige apenas número positivo (`.../dto/notification/AdClickReportRequestDto.java:11–14`); `NotificationService.notifyAdClickMilestones` itera de 2.500 até `totalClicks` com consultas/gravações (`.../service/NotificationService.java:28,85–105`).
- **Escopo:** adicional.
- **Arquivos e locais:** `AdClickReportRequestDto`, `NotificationService.notifyAdClickMilestones` e persistência de marcos.
- **Ações a executar:**
  1. Definir fonte confiável para o total de cliques e teto de avanço aceito por requisição.
  2. Processar somente marcos novos em lote limitado, preservando deduplicação.
  3. Recusar valores extremos antes de acessar o banco.
- **Testes e validação:** cobrir valor extremo, repetição e concorrência; confirmar limite de consultas e registros criados.
- **Dependências ou decisões pendentes:** regra de negócio para comprovar cliques.
- **Critério de conclusão:** uma única requisição não provoca trabalho ilimitado nem gera marcos sem validação da contagem.

### Etapa 7 — Aplicar rate limiting aos fluxos de abuso

- **Objetivo:** limitar requisições de modo consistente entre réplicas e devolver resposta previsível.
- **Achado/evidência:** A8. Não foi encontrado rate limiting conectado à cadeia HTTP; proteção em gateway externo é desconhecida. Rotas prioritárias: `/auth/login`, `/auth/register`, `/auth/refresh`, `/ai/recipes/chat` e `/transactions/checkout`.
- **Escopo:** **F2O-163**.
- **Arquivos e locais:** integração na borda ou em `SecurityConfig`/filtro próprio; `RedisConfig` se Redis armazenar contadores; configuração por ambiente e testes HTTP.
- **Ações a executar:**
  1. Definir quotas e janelas por risco: IP obtido de proxy confiável antes do login, usuário autenticado após login e orçamento separado para IA.
  2. Implementar contador compartilhado entre réplicas ou política equivalente na borda; não confiar livremente em `X-Forwarded-For` enviado pelo cliente.
  3. Retornar `429` consistente, indicar quando tentar novamente e medir bloqueios sem registrar credenciais.
- **Testes e validação:** testar consumidores isolados, limite atingido, renovação da janela, duas instâncias e preflight não bloqueado indevidamente.
- **Dependências ou decisões pendentes:** quotas aprovadas e escolha entre borda e Redis; a Etapa 6 limita abuso dentro de uma só requisição.
- **Critério de conclusão:** rotas sensíveis respeitam limites documentados e retornam `429` verificável.

### Etapa 8 — Fechar o contrato da API key e o deploy da IA

- **Objetivo:** proteger a chave de saída; criar autenticação por chave de entrada somente se isso for confirmado como requisito.
- **Achado/evidência:** A10. `FrigusAiClient` envia `X-API-Key` apenas se `FRIGUS_AI_API_KEY` estiver presente (`.../client/FrigusAiClient.java:22–30`); o Secret criado por `deploy/aws/deploy.sh:57–72` não inclui essa propriedade. A descrição parcial da F2O-163 não define a interpretação de “Proteção da API Key”.
- **Escopo:** **F2O-163**.
- **Arquivos e locais:** `FrigusAiClient`, mecanismo de segredo e script/manifests de deploy, documentação operacional e testes do cliente.
- **Ações a executar:**
  1. Confirmar com o responsável se a proteção citada na task se refere à chave da IA, à chave dos consumidores da API ou a ambas.
  2. Para a chave da IA, provisionar por mecanismo de segredos, exigir presença nos ambientes em que o serviço a exige, restringir acesso/rotação e evitar exposição em resposta, cliente ou logs.
  3. Somente se autenticação de entrada por API key for confirmada, especificar titular, escopos, armazenamento por hash, rotação, revogação e rotas antes de implementar.
- **Testes e validação:** chave fictícia no header de saída, falha controlada sem chave em perfil que a exige e ausência em logs/artefatos; eventual chave de entrada requer testes de escopo e revogação.
- **Dependências ou decisões pendentes:** definição do responsável e contrato do serviço de IA. A ação sobre chave de entrada é condicional.
- **Critério de conclusão:** interpretação do requisito documentada e fluxo correspondente comprovado de ponta a ponta.

### Etapa 9 — Integrar, executar testes e conferir a F2O-163

- **Objetivo:** comprovar as correções e conferir os requisitos descritos na F2O-163.
- **Achado/evidência:** A1–A12; os testes atuais não cobrem todos os cenários cruzados. `AuthService.java:64–69` também responde de modo diferente para usuário inexistente e senha incorreta (A12).
- **Escopo:** **F2O-163 e adicional**.
- **Arquivos e locais:** `src/test/java/com/frigus/coreapi/integration/ApiIntegrationTest.java`, testes de services/security/WebSocket, `AuthService` e documentação de endpoints.
- **Ações a executar:**
  1. Acrescentar testes com dois usuários/dois grupos para listas, itens, descartes, WebSocket e vínculos de mensagens.
  2. Cobrir CORS/preflight, CSRF conforme contrato, `429`, chave fictícia, contador extremo e TLS/cookies em homologação.
  3. Uniformizar status e resposta pública de login para credenciais inválidas, mantendo telemetria interna sem segredo; testar usuário inexistente e senha incorreta.
  4. Revisar se Swagger público e edição de receitas/ingredientes por qualquer autenticado correspondem à política do produto; só alterar se a política exigir.
  5. Executar `./mvnw clean verify -B` em CI ou `.\mvnw.cmd clean verify -B` no Windows e comparar as evidências com os requisitos descritos na F2O-163.
- **Testes e validação:** suíte verde, cenários de autorização/negação demonstrados e conferência de configuração implantada sem revelar segredos.
- **Dependências ou decisões pendentes:** Etapas 1–8 e definição do responsável sobre o contrato da API key.
- **Critério de conclusão:** testes relevantes passam, controles operam no ambiente alvo e cada critério confirmado da F2O-163 possui evidência.

## Limites da análise

O plano se baseia em leitura estática do projeto, complementada pela consulta à F2O-163 em 2026-10-08. Na análise original, não foram executados testes, exploração ou scanner de dependências, nem lidos valores de `.env` ou de `application*.properties`. O estado real de ALB/WAF, TLS, serviço de IA e segredos implantados precisa ser confirmado durante a implementação. A task consultada descreve rate limit, CORS e proteção da API key, sem subtarefas ou comentários com critérios adicionais.
