# Frigus Core API

API principal do Frigus para contas, grupos, estoque de alimentos, listas de compras, receitas, conversas, notificações, assinaturas e recursos comerciais. O serviço expõe HTTP/JSON e WebSocket STOMP, persiste dados no PostgreSQL e usa Redis para sessões e operações de autenticação.

## Início rápido

Com Docker e Docker Compose instalados, na raiz deste repositório:

```bash
docker compose up --build -d
docker compose ps
```

O Compose inicia PostgreSQL 16, Redis 7 e a API. Ele configura credenciais **apenas para desenvolvimento local** e publica as portas somente em `127.0.0.1`:

| Serviço | Endereço padrão | Variável para alterar a porta |
| --- | --- | --- |
| API | `http://localhost:8080` | `LOCAL_API_PORT` |
| PostgreSQL | `localhost:55432` | `LOCAL_POSTGRES_PORT` |
| Redis | `localhost:56379` | `LOCAL_REDIS_PORT` |

Após a inicialização, consulte [Swagger UI](http://localhost:8080/swagger-ui/index.html) ou verifique a [readiness](http://localhost:8080/actuator/health/readiness). Para acompanhar a subida e as migrações:

```bash
docker compose logs -f api
```

Para encerrar os containers, use `docker compose down`. Os dados do PostgreSQL e Redis ficam em volumes nomeados e permanecem disponíveis na próxima inicialização.

## Como o serviço funciona

| Camada | Responsabilidade |
| --- | --- |
| `controller` e `dto` | Contratos HTTP, validação de entrada e respostas. |
| `service` | Regras de negócio, autorização por usuário/grupo e processos agendados. |
| `repository` e `model` | Persistência com Spring Data JPA no PostgreSQL. |
| `mapper` | Conversão entre entidades e DTOs com MapStruct. |
| `security` e `config` | JWT, filtros de segurança, CORS, WebSocket, Redis e integrações. |
| `client` | Chamadas para Frigus AI e Brevo. |

A aplicação começa em `CoreApiApplication`. Na inicialização, o Flyway aplica as migrações de `src/main/resources/db/migration` e o Hibernate valida o esquema (`ddl-auto=validate`). O diretório `db/` contém scripts adicionais; o caminho executado automaticamente pelo Flyway é o de `src/main/resources/db/migration`.

### Recursos da API

| Área | Rotas principais |
| --- | --- |
| Acesso e conta | `/auth`, `/profile`, `/profile/sessions`, `/profile/email-verification` |
| Grupos e catálogo | `/groups`, `/group-invitations`, `/products`, `/groups/{groupId}/products` |
| Estoque e consumo | `/stocks`, `/stock-products`, `/stock-products/{stockProductId}/movements`, `/discard` |
| Compras e economia | `/shopping-lists`, `/receipts`, `/groups/{groupId}/food-purchases`, `/groups/{groupId}/money-saving` |
| Receitas e IA | `/recipes`, `/ingredients`, `/ai/recipes` |
| Comunicação | `/conversations`, `/notifications`, `/support/requests`, `/ws` |
| Planos e comercial | `/plans`, `/subscriptions`, `/transactions`, `/commercial/groups/{groupId}`, `/advertisements` |
| Administração | `/user` e operações administrativas nas áreas acima |

As rotas não têm um prefixo global como `/api`. Consulte a [especificação OpenAPI](http://localhost:8080/v3/api-docs) ou o Swagger UI para métodos, parâmetros e esquemas atualizados. O [mapa de endpoints](docs/testing/endpoint-map.md) registra uma linha de base de testes e pode não incluir recursos adicionados depois dele.

### Autenticação e WebSocket

O login (`POST /auth/login`) e o login Google (`POST /auth/google`) devolvem tokens de acesso e atualização. Envie o token de acesso nas chamadas protegidas com `Authorization: Bearer <token>`. A renovação e o logout usam `/auth/refresh` e `/auth/logout`. O serviço também emite cookies seguros nos fluxos de login; para chamadas autenticadas à API, o filtro de segurança lê o token do cabeçalho `Authorization`. O endpoint `GET /auth/csrf` fornece o token CSRF para fluxos que usam cookies.

O handshake WebSocket/SockJS ocorre em `/ws`. O cliente STOMP se autentica no quadro `CONNECT` com `Authorization: Bearer <token>`, publica em destinos com prefixo `/app` e recebe eventos em `/topic`, `/queue` ou `/user`. Os destinos de envio implementados são `/app/chat.send` e `/app/groups.send`.

## Desenvolvimento sem o container da API

É possível manter PostgreSQL e Redis no Compose e iniciar o Java localmente. Requisitos: JDK 17 e Docker com Compose. O projeto inclui Maven Wrapper; não é necessário instalar Maven separadamente.

```bash
docker compose up -d postgres redis
cp .env.example .env
```

Edite `.env` com os valores locais abaixo (e mantenha as demais integrações opcionais conforme seu ambiente):

```dotenv
DATABASE_URL=jdbc:postgresql://localhost:55432/frigus
DATABASE_USERNAME=frigus
DATABASE_PASSWORD=frigus-local-only
REDIS_URL=redis://localhost:56379
JWT_SECRET=substitua-por-um-segredo-local-com-ao-menos-32-caracteres
```

No Windows, use `Copy-Item .env.example .env` para criar o arquivo. Inicie a aplicação com `./mvnw spring-boot:run` no macOS/Linux ou `.\mvnw.cmd spring-boot:run` no Windows. O arquivo `.env` é carregado por `application.properties` e é ignorado pelo Git. Se você alterar `LOCAL_POSTGRES_PORT` ou `LOCAL_REDIS_PORT` no Compose, ajuste as URLs locais de acordo.

## Configuração

As variáveis sem padrão são necessárias para iniciar a API fora do Compose. As demais habilitam funções ou ajustam a operação. Use `.env.example` como referência de desenvolvimento e `.env.production.example` como referência para produção; nunca publique arquivos preenchidos com credenciais.

| Variável | Uso / padrão |
| --- | --- |
| `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | Conexão JDBC com PostgreSQL; obrigatórias. |
| `REDIS_URL` | Conexão com Redis; obrigatória. A aplicação usa Redis também para sessões. |
| `JWT_SECRET` | Assinatura dos JWTs; obrigatória. Use um segredo aleatório com ao menos 32 caracteres. |
| `JWT_EXPIRATION`, `PORT` | Expiração do token de acesso em milissegundos (`900000`) e porta HTTP (`8080`). |
| `SECURITY_ALLOWED_ORIGINS` | Origens aceitas por CORS e WebSocket; padrão `http://localhost:3000,http://localhost:5173`. |
| `APP_FRONTEND_URL`, `APP_TIME_ZONE` | URL usada em fluxos de e-mail (`http://localhost:5173`) e fuso das tarefas (`America/Sao_Paulo`). |
| `FLYWAY_ENABLED`, `FLYWAY_BASELINE_ON_MIGRATE`, `FLYWAY_BASELINE_VERSION` | Migrações (`true`), baseline automático (`false`) e versão de baseline (`3`). Revise o banco antes de habilitar baseline. |
| `FRIGUS_AI_BASE_URL`, `FRIGUS_AI_API_KEY`, `FRIGUS_AI_API_KEY_REQUIRED` | Origem da IA (`http://localhost:8000`), chave usada pelo Core nos serviços da IA e exigência de chave na inicialização (`false`). O valor de `FRIGUS_AI_API_KEY` deve corresponder a `API_KEYS__CORE_SERVICE_KEY` no serviço frigus-ai para autenticar o chat interno. |
| `BREVO_API_URL`, `BREVO_API_KEY`, `BREVO_SENDER_EMAIL`, `BREVO_SENDER_NAME` | API de e-mail (`https://api.brevo.com/v3`) e remetente; chave e e-mail são necessários para enviar mensagens. |
| `GOOGLE_CLIENT_ID` | Público esperado no login com Google; necessário para essa função. |
| `AWS_REGION`, `AWS_S3_ENABLED` | Região (`sa-east-1`) e criação opcional do cliente S3 (`false` no Compose). `AWS_S3_BUCKET` aparece no exemplo de ambiente, mas não é lida pelo código atual. |
| `AD_CLICK_REPORT_SECRET` | Segredo de autenticação dos relatórios de cliques em anúncios. |
| `SECURITY_RATE_LIMIT_AUTH`, `SECURITY_RATE_LIMIT_AI`, `SECURITY_RATE_LIMIT_CHECKOUT`, `SECURITY_RATE_LIMIT_WINDOW_SECONDS` | Limites padrão `10`, `20`, `10` requisições por janela de `60` segundos. |
| `SECURITY_TRUSTED_PROXY_CIDRS` | CIDRs dos proxies cujos cabeçalhos de IP podem ser considerados pelo limitador; vazio por padrão. |
| `NOTIFICATIONS_EXPIRATION_CRON`, `NOTIFICATIONS_EXPIRATION_WARNING_DAYS` | Avisos de validade: `0 0 9 * * *` e `3` dias. |
| `NOTIFICATIONS_SHOPPING_LIST_REMINDER_CRON`, `NOTIFICATIONS_SHOPPING_LIST_REMINDER_DELAY_DAYS` | Lembretes de lista: `0 0 10 * * *` e `3` dias. |
| `notifications.weekly-summary.cron`, `billing.job.cron` | Propriedades Spring para resumo semanal (`0 0 9 * * MON`) e cobrança (`0 0 3 * * ?`). |

`application.properties` também define `CORS_ALLOWED_ORIGINS` para a propriedade `app.cors.allowed-origins`; os componentes atuais de CORS e WebSocket consultam `SECURITY_ALLOWED_ORIGINS`. Configure esta última ao alterar as origens permitidas.

No Compose, as variáveis `LOCAL_*` ajustam portas e credenciais locais; `LOCAL_FRONTEND_URL` e `LOCAL_AI_BASE_URL` definem os endereços de frontend e IA vistos pelo container. A configuração do Compose substitui as URLs de banco e Redis da máquina anfitriã pelos nomes de serviço `postgres` e `redis`.

## Operação e integrações

- **Saúde:** `/actuator/health`, `/actuator/health/liveness` e `/actuator/health/readiness` são públicos. O Compose usa readiness como health check.
- **Banco:** o Flyway roda no startup. Faça backup e revise migrações pendentes antes de apontar uma nova versão para um banco persistente de produção.
- **Tarefas agendadas:** a aplicação executa notificações de validade, lembretes de compras, resumo semanal, cobrança de assinaturas e processamento de transações. Os cron jobs de notificações usam `APP_TIME_ZONE`.
- **IA:** os recursos de chat de receitas e extração de notas chamam Frigus AI. Sem o serviço externo ou a chave adequada, essas funções podem falhar mesmo quando a API está saudável.
- **E-mail e Google:** recuperação de senha e verificação de e-mail dependem do Brevo; login Google depende de `GOOGLE_CLIENT_ID`.
- **S3:** o cliente AWS é criado quando `aws.s3.enabled=true`; configure a região e as credenciais pelo mecanismo da infraestrutura (por exemplo, role do workload).

Para executar a suíte existente: `./mvnw test` no macOS/Linux ou `.\mvnw.cmd test` no Windows. Os testes comuns usam H2 e desativam Flyway e sessão Redis; alguns testes de integração usam Testcontainers e exigem Docker.

## Deploy e documentos relacionados

O projeto inclui um [Dockerfile](Dockerfile) multiestágio e manifests em [`deploy/k8s`](deploy/k8s/README.md). O [guia de deploy na AWS](deploy/k8s/README.md) descreve a publicação conjunta da Core API e do BFF em EKS por meio de `deploy/aws/deploy.sh`, incluindo dependências, segredos, verificações e atualização de versões. Nesse desenho, a Core API fica em um serviço privado do cluster e o BFF recebe o tráfego público.

Consulte também a [operação F2O-163](docs/f2o-163-operacao.md), os [diagnósticos da integração de IA](docs/ai-chat-provider-diagnostics.md) e os demais documentos em [`docs/`](docs/). Esses arquivos aprofundam fluxos específicos; o contrato HTTP atual pode ser consultado no OpenAPI da aplicação em execução.
