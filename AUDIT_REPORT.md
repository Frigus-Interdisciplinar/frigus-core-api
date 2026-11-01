# Relatório de auditoria — Core API

**Data:** 31/10/2026
**Branch:** `feat/audit`
**Escopo:** API Java/Spring Boot, com foco em segurança, dependências, validação, tratamento de erros, consultas e estado de build/testes.

## Resumo

A auditoria encontrou dependências em versões afetadas por advisories de segurança, validações ausentes em dois contratos HTTP, formatos de erro inconsistentes entre filtros e controllers e três padrões de consultas N+1. As correções foram aplicadas nesta branch. A suíte final da branch passou com 375 testes.

Não encontrei evidência, nos trechos revisados, de credenciais embutidas, SQL nativo montado com entrada não parametrizada ou controllers retornando entidades JPA diretamente. A revisão foi estática e direcionada; não substitui análise dinâmica ou pentest.

## Achados

| ID | Severidade | Achado e impacto | Correção / estado |
|---|---|---|---|
| SEC-01 | **Alta — dependência** | `spring-data-commons` estava em 4.0.5, versão afetada por CVE-2026-41695, que permite exaustão de recursos quando caminhos de propriedade controlados por usuário chegam à resolução do `MappingContext`. A exposição depende do módulo/código consumidor; não identifiquei esse fluxo no código revisado. | Atualizado para 4.0.6. A versão corrigida é indicada pelo [advisory oficial do Spring](https://spring.io/security/cve-2026-41695/). |
| SEC-02 | **Média — dependência** | A mesma versão de Spring Data era afetada por CVE-2026-41721, DoS condicionado ao uso de Spring Data Web com método controller anotado com `@ProjectedPayload`. Não encontrei essa anotação no código. | Atualizado junto com SEC-01 para 4.0.6, versão corrigida conforme [advisory oficial do Spring](https://spring.io/security/cve-2026-41721/). |
| SEC-03 | **Alta/Média — dependência, uso condicionado** | `jackson-databind` e `jackson-core` estavam resolvidos em 2.21.2. A faixa incluía CVE-2026-54512 (validação incompleta de parâmetros genéricos em `PolymorphicTypeValidator`, severidade alta) e CVE-2026-54514, CVE-2026-54517 e CVE-2026-54518, que atingem caminhos específicos de desserialização. A busca no código não encontrou ativação de default typing nem `@JsonTypeInfo`; os outros cenários requerem recursos/modelos específicos e não foram confirmados como exploráveis pela API. | Atualizados para 2.21.8. A versão 2.21.4 já corrigia esses CVEs, conforme as [notas de release oficiais do Jackson](https://github.com/FasterXML/jackson/wiki/Jackson-Release-2.21.4); 2.21.8 é a versão selecionada no patch da auditoria. Consulte também o [registro NVD de CVE-2026-54512](https://nvd.nist.gov/vuln/detail/CVE-2026-54512). |
| SEC-04 | **Média — dependência, uso condicionado** | Bouncy Castle 1.79 estava abaixo das versões corrigidas para CVE-2026-0636 (API LDAP/X.509) e CVE-2026-5598 (decriptação FrodoKEM com diferença de tempo). Esses problemas exigem o uso explícito das APIs afetadas; não encontrei chamadas a elas no código da aplicação. | Atualizado para 1.86. As condições e versões corrigidas estão nos [avisos do projeto para CVE-2026-0636](https://github.com/bcgit/bc-java/wiki/CVE%E2%80%902026%E2%80%900636) e [CVE-2026-5598](https://github.com/bcgit/bc-java/wiki/CVE%E2%80%902026%E2%80%905598). |
| SEC-05 | **Alta** | O segredo JWT não era validado no início da aplicação. Um segredo curto enfraquece HMAC-SHA-256 e uma expiração inválida causa tokens sem a política esperada. | `TokenProvider` agora falha no startup se `JWT_SECRET` tiver menos de 32 bytes UTF-8 ou se a expiração não for positiva; geração e verificação usam a mesma validação do algoritmo. |
| SEC-06 | **Média** | Os filtros de autenticação/rate limit e o handler MVC retornavam formatos de erro diferentes. O rate limit também não convertia indisponibilidade do Redis em uma resposta controlada. | Respostas de autenticação, autorização, rate limit e exceções usam `application/problem+json`, com campos RFC 9457 e extensões legadas usadas pelo BFF. Indisponibilidade do Redis resulta em 503 sem expor detalhes internos. |
| SEC-07 | **Média** | Os DTOs de compra não validavam campos obrigatórios, quantidades, valores ou linhas aninhadas; a reação de mensagem aceitava um `Map` livre sem limite de tamanho para o emoji. | Aplicadas validações Jakarta com `@Valid` nos endpoints e criado `MessageReactionRequestDto` limitado a 16 caracteres. |
| PERF-01 | **Média** | O relatório de economia consultava as alocações de cada evento de saída em separado, gerando N+1. | Alocações agora são consultadas em lote para os IDs da página. |
| PERF-02 | **Média** | O histórico de mensagens consultava anexos uma vez por mensagem. | Anexos de toda a página são carregados em uma consulta e agrupados por mensagem. |
| PERF-03 | **Média** | A listagem de conversas carregava participantes e última mensagem por conversa. | Participantes e últimas mensagens são buscados em lote para as conversas retornadas. |
| QUAL-01 | **Média** | O fixture `db/script.sql` não incluía colunas de usuário adicionadas pelas migrations V017 e V018. Isso gerava dois erros na suíte de linha de base. | Fixture alinhado com `email_verified`, `plan_selection_required` e `pending_plan_transaction_id`. A suíte passou depois da correção. |
| PERF-04 | **Baixa — residual** | `User` e `Subscription` ainda têm relacionamentos `EAGER`. Os caminhos revisados usam esses dados e não houve N+1 confirmado associado, mas o carregamento global pode trazer dados sem necessidade em outros caminhos. | Mantido sem alteração por depender de avaliação mais ampla de consumidores e planos de consulta; considerar migração gradual para `LAZY` com medição de regressões.

## Verificações realizadas

- Revisão de autenticação JWT, carregamento de usuário, uso de versão de token, filtros de segurança e autorização observável nos fluxos de grupos/conversas.
- Busca direcionada por segredos em código, SQL nativo com entradas concatenadas, desserialização polimórfica, `@ProjectedPayload` e relacionamentos `EAGER`.
- Inspeção das consultas de histórico de mensagens, listagem de conversas e relatório de economia para localizar consultas por item.
- Árvore Maven final confirmou `spring-data-commons:4.0.6`, `jackson-core:2.21.8`, `jackson-databind:2.21.8` e `bcprov-jdk18on:1.86`.
- Não foi confirmado fluxo de injeção SQL nos trechos revisados; as consultas encontradas usam parâmetros nomeados/posicionais ou filtros construídos a partir de opções controladas.

## Arquivos alterados pela auditoria

- **Dependências e fixture:** `pom.xml`, `db/script.sql`.
- **Segurança e erros:** `src/main/java/com/frigus/coreapi/security/TokenProvider.java`, `SecurityConfig.java`, `RateLimitFilter.java`, `SecurityFilter.java`; `src/main/java/com/frigus/coreapi/exception/GlobalExceptionHandler.java`; `src/main/java/com/frigus/coreapi/dto/ErrorResponseDto.java`; `src/main/java/com/frigus/coreapi/service/AuthService.java`.
- **Validação de contratos:** `src/main/java/com/frigus/coreapi/dto/moneysaving/MoneySavingDtos.java`, novo `src/main/java/com/frigus/coreapi/dto/message/MessageReactionRequestDto.java`, `src/main/java/com/frigus/coreapi/controller/MoneySavingController.java`, `ConversationController.java`.
- **Consultas em lote:** `src/main/java/com/frigus/coreapi/service/MoneySavingReportService.java`, `MessageService.java`, `ConversationService.java`; `src/main/java/com/frigus/coreapi/repository/MessageAttachmentRepository.java`, `ConversationParticipantRepository.java`, `ConversationRepository.java`, `MessageRepository.java`.
- **Teste:** `src/test/java/com/frigus/coreapi/exception/GlobalExceptionHandlerTest.java` ganhou verificações para Problem Details e sanitização da resposta genérica 500.

## Linha de base e resultado final

| Execução | Resultado |
|---|---|
| Inicial — `./mvnw clean test` | 375 testes; 0 falhas e 2 erros. Ambos eram erros de fixture por colunas ausentes (`email_verified` e campos de seleção de plano). |
| Final — `./mvnw clean test` | **375 testes; 0 falhas, 0 erros, 0 ignorados. `BUILD SUCCESS`.** |

## Estado do worktree

A branch `feat/audit` foi criada a partir da branch de trabalho existente. Alterações locais já presentes no início foram preservadas, sem serem atribuídas à auditoria: `.gitignore`, `deploy/aws/deploy.sh`, arquivos em `deploy/k8s/`, `docs/f2o-163-operacao.md`, `ProfileController.java`, `UserProfileService.java`, `.env.production.example` e `deploy/aws/prod.env.example`. Também foram observadas durante a tarefa alterações em `RestClientConfig.java`, `GoogleTokenVerifierService.java` e `GoogleTokenVerifierServiceTest.java`; elas foram mantidas intactas e não fazem parte deste relatório de alterações da auditoria.
