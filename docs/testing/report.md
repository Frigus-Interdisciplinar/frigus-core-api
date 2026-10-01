# Análise e complementação dos testes da API

## Codebase analisada

- Java 17, Maven Wrapper, Spring Boot 4.0.6, Spring MVC, Spring Security, JPA/Hibernate, Lombok e MapStruct. O `package.json` serve ao Husky/Commitlint; `npm test` não é a suíte da API.
- Inicialização: `CoreApiApplication`, com agendamento habilitado. Fluxo HTTP: filtros de segurança/CORS → controller e DTO/Bean Validation → service → repository/JPA ou integração → mapper/JSON; `GlobalExceptionHandler` traduz exceções. Controllers que estendem `BaseController` também herdam leitura/exclusão por ID.
- PostgreSQL: schema oficial em `db/script.sql`, enums nativos, chaves estrangeiras, índices únicos, funções e triggers. As migrations existentes são scripts SQL manuais; não há Flyway/Liquibase configurado. H2 em modo PostgreSQL é o banco do teste de contexto original.
- Autenticação: JWT HMAC/issuer `frigus`, Bearer ou cookie `accessToken`; papéis consultados no banco; Argon2; refresh tokens no Redis. Autorização administrativa por `@PreAuthorize` e escopo de grupo/participação nos services.
- Redis: refresh tokens e fila de transações, consumidor agendado, retry e cobrança recorrente. Chat usa STOMP/WebSocket e broker em memória. IA usa `RestClient` com fallback de receita; S3 é condicional e não há operação de upload implementada na API analisada.
- OpenAPI/Swagger gerado por Springdoc; Actuator com health/probes. Docker em dois estágios, Kubernetes e script de deploy AWS. CI: JDK 17, PostgreSQL 16/Redis 7, schema SQL, `./mvnw clean verify -B`, validação de manifests e build Docker.
- JUnit Jupiter 6, Mockito, AssertJ, Spring Test/MockMvc e Spring Security Test. Novos testes de persistência usam Testcontainers PostgreSQL, com versões gerenciadas pelo Spring Boot.
- Não havia `AGENTS.md` nem README no repositório. Foram lidos os arquivos de configuração, schema/migrations, documentação existente, controllers, services, models, DTOs, segurança, repositories e testes. A branch não foi trocada e não havia alterações locais na linha de base.

## Situação inicial

```text
Comando: ./mvnw.cmd clean verify -B
JAVA_HOME: JDK 17 instalado na máquina (o java padrão era 8)
Arquivos de teste: 47
Testes encontrados: 212
Aprovados: 212
Falhos: 0
Ignorados: 0
Erros: 0
Duração do Maven: 4min06s
Resultado: BUILD SUCCESS
```

A suíte original tem testes unitários de services, controllers, mappers, segurança, exceptions, models e configuração, além de um `contextLoads`. Não há fixtures/factories compartilhadas; os dados são montados com builders locais e os repositories geralmente são mocks.

Os services já protegiam regras como idade mínima, limite de membros, titularidade na consulta de transação, recusa de pagamento, retry e cancelamento. Entretanto, várias asserções se limitavam a `isNotNull`, tamanho de lista, retorno do mock ou delegação. Alguns testes agrupavam vários comportamentos independentes numa mesma função. Testes de controller invocados diretamente não passam por filtros, proxies de autorização, validação, serialização ou transações. O teste de catálogo com MockMvc standalone não incluía o advice global nem autorização real. As anotações de permissão eram verificadas por reflexão, sem demonstrar o bloqueio HTTP.

Não foram encontradas anotações de testes ignorados/desabilitados. Não há JaCoCo, linter Java ou análise estática/tipos separada configurados; o compilador do Maven verifica tipos. Os hooks executam `mvn compile` e Commitlint. Não foi introduzida uma ferramenta de cobertura apenas para produzir uma porcentagem.

Apesar do sucesso inicial, o H2 registrou erros de DDL por enums PostgreSQL desconhecidos e tabelas ausentes. O teste de contexto não verifica acesso às tabelas, portanto esse resultado não provava persistência funcional. Classificação: **PROBLEMA DE AMBIENTE da infraestrutura original de testes**.

## Mapa e priorização

O [mapa completo dos endpoints](endpoint-map.md) registra 87 operações HTTP, com método, rota, regra, autenticação, testes iniciais, lacuna e prioridade. Rotas herdadas foram incluídas; não foram criados endpoints fictícios.

| Área | Cenário ausente | Prioridade | Teste implementado |
|---|---|---|---|
| JWT/HTTP | Sem credencial, token inválido, cookie, precedência do Bearer, usuário excluído e papel persistido | P1 | `ApiIntegrationTest` |
| JWT | Expiração, issuer e assinatura incompatíveis | P1 | `TokenProviderTest` |
| Administração | Usuário comum bloqueado em leitura/admin e criação de catálogo; administrador executa CRUD | P1 | `ApiIntegrationTest` |
| Cadastro/login | Persistência real, hash Argon2, resposta sem senha, cookies e duplicidade | P1 | `ApiIntegrationTest` |
| Refresh | Ausência de token retorna 401 sem cookies | P1 | `ApiIntegrationTest` |
| Grupos | Sem autenticação, exceção administrativa, membro versus estranho, grupo já ativo/dono e plano gratuito | P1 | `GroupAccessServiceTest`, `GroupServiceTest` |
| Estoques | Leitura/listagem/transferência/exclusão entre grupos; CRUD e nome normalizado no banco | P1 | `ApiIntegrationTest` |
| Itens em estoque | CRUD, lote duplicado, vínculos inexistentes, escopo e status derivado | P1/P2 | `ApiIntegrationTest` |
| Movimentações | Entrada/saída/ajuste, saldo zero, overflow, negativos, saldo insuficiente e auditoria | P1 | `ApiIntegrationTest` |
| Transações de estoque | Falha real de constraint reverte saldo e auditoria | P1 | `ApiIntegrationTest` |
| Checkout | Idempotência, envio após commit, titularidade, plano gratuito e cancelamento persistidos | P1 | `ApiIntegrationTest` |
| Integridade PostgreSQL | Unicidade do lote e FK impedindo exclusão de produto referenciado | P1/P2 | `ApiIntegrationTest` |
| Migration de plano FREE | Atualização da constraint antiga, reaplicação e rejeição de valores negativos | P1 | `ApiIntegrationTest` |
| Validação/erros HTTP | Obrigatório/nulo/vazio, JSON e enum inválidos, limites de preço/nome, filtros e ID malformado; sem detalhes internos | P2 | `ApiIntegrationTest` |
| Catálogo | Consulta pública, paginação e campos numéricos reais | P2 | `ApiIntegrationTest` |
| Conversas | Sem login, DM consigo mesmo e adição em conversa privada | P1/P2 | `ConversationServiceTest` |
| Mensagens | Participante que saiu, acesso indevido ao histórico/read receipt, falta de destino/login, recibo repetido | P1/P2 | `MessageServiceTest` |
| WebSocket | Headers suportados, token ausente/inválido, usuário excluído, SEND não troca identidade | P1 | `WebSocketAuthInterceptorTest` |
| IA | Criação/reutilização de sessão, contrato JSON, resposta inválida, indisponibilidade, timeout e fallback | P2 | `FrigusAiClientTest` |

## Testes adicionados por arquivo

- `src/test/java/com/frigus/coreapi/integration/ApiIntegrationTest.java`: integração HTTP/JPA com filtros, JWT real, services, repositories, serializers e schema oficial. PostgreSQL descartável inicializado com `db/script.sql`, dados mínimos recriados entre cenários, sem transação externa envolvendo a requisição. Falhas são verificadas também pela ausência de alteração no banco. Redis é substituído nos limites de refresh/fila; jobs agendados são substituídos para impedir consumo externo ou alterações em segundo plano. Nenhuma camada de persistência interna é mockada.
- `src/test/java/com/frigus/coreapi/client/FrigusAiClientTest.java`: contrato de integração externa com transporte HTTP em memória, requests/responses reais serializados, verificação de método/URL/body, sucesso e fallback. Timeout é uma `SocketTimeoutException` simulada, sem espera de relógio. O cliente existente recebe o transporte de teste por `ReflectionTestUtils`, sem mudança de produção.
- `src/test/java/com/frigus/coreapi/security/WebSocketAuthInterceptorTest.java`: unitário de autenticação STOMP com JWT real e repository mockado; quatro variações de header e caminhos negativos.
- `src/test/java/com/frigus/coreapi/service/GroupAccessServiceTest.java`: unitário de autorização de grupo; limpa `SecurityContextHolder` entre testes e verifica rejeição antes de consulta indevida.
- `src/test/java/com/frigus/coreapi/security/TokenProviderTest.java`: amplia os testes existentes com tokens expirados/de outro emissor/outra assinatura, validando ambos os métodos públicos de verificação.
- `src/test/java/com/frigus/coreapi/service/GroupServiceTest.java`: amplia os builders/mocks já existentes para rejeições de criação, verificando ausência de escrita de grupo/conversa/participantes.
- `src/test/java/com/frigus/coreapi/service/MessageServiceTest.java`: amplia os testes existentes com rejeições de acesso e idempotência de recibo, sem publicação ou persistência indevida.
- `src/test/java/com/frigus/coreapi/service/ConversationServiceTest.java`: amplia testes existentes com autenticação e restrições de conversas privadas, sem consultas/escritas desnecessárias.

## Execuções e classificação

A primeira execução de integração encontrou 51 erros de contexto, sem conseguir executar os cenários. A causa raiz foi `NoClassDefFoundError: FrigusAiPromptPayload`; as classes de `target` estavam sendo recompiladas por processos Java da IDE durante a execução. Classificação: **PROBLEMA DE AMBIENTE**. O Maven passou a aceitar `-Dmaven.build.directory=.test-build`, preservando `target` como padrão. A saída isolada é ignorada pelo Git. Processos da IDE não foram interrompidos.

A execução focada antes das correções executou 113 cenários: **84 aprovados, 29 falhos, zero erros e zero ignorados**, em 12min42s. As falhas de integração reproduziram os defeitos da tabela abaixo; os testes unitários novos e os oito cenários de IA passaram. Evidência por cenário: `regressions-before-fixes.txt`.

A execução focada após as correções executou 147 cenários dos módulos afetados: **147 aprovados, zero falhas, erros ou ignorados**, em 2min20s (`BUILD SUCCESS`). Comando:

```powershell
./mvnw.cmd -Dmaven.build.directory=.test-build -Dtest=ApiIntegrationTest,ProductServiceTest,StockProductServiceTest,StockMovementServiceTest,TransactionServiceTest,GlobalExceptionHandlerTest,FrigusAiClientTest,WebSocketAuthInterceptorTest,TokenProviderTest,GroupAccessServiceTest,GroupServiceTest,MessageServiceTest,ConversationServiceTest test -B
```

## Defeitos comprovados e correções mínimas

Todos os itens abaixo são **DEFEITO ENCONTRADO NA API**, já presentes no código inicial, mas não detectados pelos 212 testes originais. Não houve mudança de regra para acomodar testes: foram preservados os contratos e comportamentos definidos no próprio código/DTO/schema.

| Localização | Comportamento anterior | Esperado e sustentação | Severidade | Evidência e correção |
|---|---|---|---|---|
| `TransactionService.checkout` | Reutilização de chave de outro usuário devolvia HTTP 200 com a transação alheia | Negar acesso, como já faz `getTransactionById`/cancelamento por titularidade | Crítica | `checkoutCannotReuseAnotherUsersIdempotencyKey`: 403 esperado, 200 recebido. Adicionada verificação do titular antes de retornar transação existente |
| `GlobalExceptionHandler` | `AccessDeniedException` de `@PreAuthorize` virava 500 | 403 sem escrita; anotações existentes restringem acesso a ADMIN | Importante | Cinco cenários de autorização esperavam 403 e recebiam 500. Handler específico agora retorna o formato `FORBIDDEN` existente |
| `GlobalExceptionHandler` | DTO inválido, JSON malformado, enum inválido, filtro/ID incorreto eram capturados como 500 | 400, conforme Bean Validation e resolução MVC; mensagens sem detalhes internos | Importante | Cenários de payloads/limites negativos e formatos inválidos. Handlers específicos preservam mensagens de validação e omitem valores rejeitados/stack traces |
| `Product`, `StockProduct`, `StockMovement` | Escritas falhavam porque VARCHAR era enviado a colunas enum PostgreSQL | Persistir os valores dos enums já definidos no schema | Alta | CRUD e cinco tipos/limites de movimentação esperavam 201 e recebiam 500; PostgreSQL indicava tipos incompatíveis. Adicionado `PostgreSQLEnumJdbcType`, seguindo a convenção já usada em User/Plan/Transaction |
| `StockService.create` | Estoque novo tinha timestamps nulos e falhava na validação da entidade | Criar estoque com `createdAt`/`updatedAt` obrigatórios, como outros services já fazem | Alta | `createsUpdatesAndDeletesStockWithPersistedNormalizedName`: 201 esperado, 500 por validação durante persistência. Timestamps inicializados no service |
| `db/script.sql` | `amount > 0` impedia ativação do plano FREE implementada no service | Permitir zero; continuar proibindo negativos | Alta | `freeCheckoutActivatesSubscriptionWithoutQueueAndPersistsZeroAmount`: 200 esperado, 500 por `transactions_amount_check`. Schema passa a `>= 0`; migration 003 atualiza bancos existentes |

Foi usada a skill `surgical-patch` para restringir as alterações aos mecanismos responsáveis. Nenhum banco fora dos containers descartáveis recebeu schema, migration ou dados. A migration de produção é entregue como arquivo para o processo normal de implantação do projeto.

## Arquivos alterados

| Arquivo | Alteração |
|---|---|
| `.gitignore` | Ignora a saída de build isolada e logs completos de validação local |
| `pom.xml` | Dois módulos de Testcontainers com escopo test; diretório Maven sobrescrevível, mantendo `target` como padrão |
| `db/script.sql` | Permite valor zero de transação FREE, proibindo negativos |
| `db/migrations/003_allow_free_transactions.sql` | Atualização transacional/reaplicável da constraint de bancos existentes |
| `src/main/java/com/frigus/coreapi/exception/GlobalExceptionHandler.java` | Traduz negação de acesso e erros de entrada para 403/400 |
| `src/main/java/com/frigus/coreapi/model/Product.java` | Binding de três enums PostgreSQL |
| `src/main/java/com/frigus/coreapi/model/StockProduct.java` | Binding de dois enums PostgreSQL |
| `src/main/java/com/frigus/coreapi/model/StockMovement.java` | Binding do enum de movimentação |
| `src/main/java/com/frigus/coreapi/service/StockService.java` | Inicializa timestamps exigidos pela entidade |
| `src/main/java/com/frigus/coreapi/service/TransactionService.java` | Confere titularidade de chave de idempotência reutilizada |
| Oito arquivos de testes descritos acima | Quatro classes novas e ampliação de quatro classes existentes |
| `docs/testing/endpoint-map.md` | Mapa das 87 operações HTTP e lacunas iniciais |
| `docs/testing/report.md` | Arquitetura, baseline, cenários, evidências, classificação, correções e limitações |
| `docs/testing/regressions-before-fixes.txt` | Relatório Surefire com os 29 casos falhos antes das correções |
| `docs/testing/execution-evidence.txt` | Resumos dos comandos Maven e contagens por classe nas execuções inicial, focadas e final |

## Resultado final

```text
Comando: ./mvnw.cmd clean verify -B -Dmaven.build.directory=.test-build
Arquivos de teste: 51
Testes encontrados: 308
Aprovados: 308
Falhos: 0
Ignorados: 0
Erros: 0
Duração do Maven: 2min51s
Resultado: BUILD SUCCESS
Código de saída do Maven: 0
```

Foram adicionados **96 cenários**, em quatro classes novas e quatro classes ampliadas. Os 212 testes originais continuam presentes. Os 62 cenários de integração passaram com PostgreSQL real; a migration foi validada sobre a constraint antiga e reaplicada, permitindo zero e rejeitando negativos. O teste de rollback confirma especificamente a falha da constraint de auditoria, não qualquer erro 500. A verificação de publicação após commit usa uma conexão separada para comprovar visibilidade do registro já confirmado.

`clean verify` executou a compilação Java, a suíte completa, o empacotamento e o repackage Spring Boot, como no CI. A única diferença do comando local é a pasta de saída isolada. `git diff --check` passou. Não há linter Java ou verificação de tipos independente configurados. Não foram removidos, desabilitados ou ignorados testes; não foram encontradas falhas de asserção preexistentes na linha de base, nem erros nos novos testes ao final.

Os [resumos de execução](execution-evidence.txt) preservam os resultados por classe; [as regressões anteriores às correções](regressions-before-fixes.txt) preservam cenários e asserções. Logs completos estão em `.test-evidence/` (ignorados pelo Git); relatórios XML/TXT do último run estão em `.test-build/surefire-reports/`. Ambos ficam dentro do repositório. O Docker é necessário para executar as novas integrações; Testcontainers cria portas dinâmicas e remove os containers ao encerrar a JVM.

## Limitações reais e riscos identificados na análise

- O teste original com H2 não exercita o schema PostgreSQL nem suas constraints/triggers. A nova integração cobre esses mecanismos diretamente, mas a infraestrutura H2 original continua limitada.
- O interceptor WebSocket trata CONNECT e não aplica autorização a SUBSCRIBE. Os testes novos verificam autenticação; não provam isolamento dos tópicos de assinatura. É necessário definir/aplicar a política de inscrição por conversa/grupo.
- `DiscardService` não usa `GroupAccessService`; leitura/listagem/exclusão herdadas e criação não verificam associação ao grupo do estoque. Não existe documentação específica de permissão para descartes. A política precisa ser explicitada; o contraste com estoques escopados exige revisão prioritária.
- A API não possui um contrato OpenAPI versionado separado do código. Os testes HTTP verificam contratos observáveis dos cenários implementados, sem alegar conformidade integral de todas as 87 operações.
- Redis real e AWS S3 não são chamados nos novos cenários; a fila/refresh têm testes unitários já existentes e fronteiras mockadas na integração. Não há teste de concorrência multithread ou de entrega durável da fila após indisponibilidade do Redis.
- Não há lint Java separado configurado. Build/compilação e testes usam Maven; validações de Kubernetes/Docker não substituem a suíte da API.
