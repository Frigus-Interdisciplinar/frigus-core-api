# Correção da fila de transações e seleção de assinatura

Base do artefato: `32c95fd3d9ed8bb12a349137a16a76b0be90f3f0` (`origin/dev`). Patch independente: `subscription-queue-recovery.patch`. Preparado em checkout detached separado, sem aplicar ao banco, sem processar cobrança e sem integrar o patch de IA.

## Diagnóstico sustentado pelo código

A exceção apresentada ocorre no worker agendado, não no controlador HTTP de IA. `TransactionWorkerService.consumeQueue` consome uma mensagem, chama `TransactionProcessorService.processTransaction` e, se houver exceção, chama `handleProcessingFailure` antes de reenfileirar. O agendamento tem intervalo de um segundo e o retry existente não usa backoff.

O processor incrementava `attempts` dentro da mesma transação que procurava a assinatura. `findByUserId` retorna um `Optional` mas não filtra `deleted_at`: mais de uma linha do usuário causa exceção. O rollback desfaz status PROCESSING, incremento e evento. O handler consultava o contador anterior e não o persistia; para um registro iniciado em zero, continuava zero, abaixo do máximo três, indefinidamente. Vários IDs podem repetir intercalados porque cada payload é devolvido à fila. A consulta utiliza o usuário da transação; não é uma busca global de todas as assinaturas.

O defeito de tentativas já aparece em `0e36897`. O método `findByUserIdAndDeletedAtIsNull` foi adicionado em `0ff2007`, mas os caminhos de pagamento e checkout FREE continuaram usando a consulta anterior. Nenhum fix posterior foi encontrado nas referências locais inspecionadas, inclusive `origin/dev`.

A evidência do log não permite identificar quais assinaturas são duplicadas nem se os vários IDs pertencem ao mesmo usuário. Nenhuma consulta de produção foi realizada: não havia identificador confiável da transação na imagem disponível para restringir a leitura.

## Alterações delimitadas

- Processor e checkout FREE usam o método existente `findByUserIdAndDeletedAtIsNull`.
- As duas entradas do processor leem a transação com lock pessimista por ID. Mensagem duplicada não deve aprovar uma transação terminal nem sobrescrever um contador concorrente.
- O handler usa transação própria `REQUIRES_NEW`, aceita somente PENDING e persiste o contador anterior mais um. As três falhas técnicas passam a resultar em 1, 2 e 3, encerrando a terceira com transação ERROR e evento.
- Uma exceção técnica não transforma uma assinatura associada em DELINQUENT. A assinatura e seu período ficam intactos no handler. Essa mudança é explícita: indisponibilidade/ambiguidade de dados não comprova inadimplência. O ramo anterior de recusa explícita do gateway permanece inalterado.
- Nenhuma alteração no worker, regras de preço/período, recorrência, checkout/idempotência ou estado de assinatura é feita fora dessas linhas. O patch não executa SQL de saneamento.

## Limite da política existente

O índice parcial `uq_subscriptions_one_active_per_user`, tanto no schema antigo quanto em `B003__core_schema.sql`, impede mais de uma TRIAL/ACTIVE não deletada por usuário. Ele permite histórico EXPIRED/CANCELED e linhas deletadas. Não garante que todas as linhas com `deleted_at IS NULL` sejam únicas.

Assim, uma assinatura não deletada e outra deletada deixam de causar ambiguidade por esse motivo. Duas não deletadas continuam falhando explicitamente, inclusive ACTIVE + EXPIRED/CANCELED, sem escolher arbitrariamente a primeira, apagar histórico ou reativar uma assinatura antiga. O patch limita a repetição técnica, mas não decide uma nova política de seleção de histórico. As demais rotas `SubscriptionService` também usam `Optional` com DeletedAtIsNull e precisam de revisão de política se esse cenário existir. O mapeamento `User.subscription` é OneToOne e também não representa múltiplas linhas históricas; não foi alterado neste bugfix.

Transações já terminais não são retomadas automaticamente. Não há reenvio, alteração de status ou correção de dados existentes pelo artefato.

## Validação

`git apply --check` passou em checkout limpo da base exata. `git apply --reverse --check` passou no checkout preparado; `git diff --check` passou. Após validar o artefato, os mesmos ajustes cirúrgicos foram espelhados no checkout de trabalho baseado em `f824262`, nos três arquivos de produção e três testes correspondentes, sem substituir arquivos inteiros de `origin/dev`. SHA256 dos 13 arquivos anteriores de IA e integração financeira confirmou preservação integral do WIP.

Os testes novos usam JDBC H2 e interceptação transacional Spring reais, com repositórios controlados pelos testes, sem conexão com banco de produção nem worker agendado. A fixture SQL de assinatura modela o contrato do método derivado existente; não é uma validação do schema real de produção.

Cobrem rollback do PROCESSING/contador/evento, persistência das três falhas, ausência de alteração da assinatura ACTIVE no erro técnico, parada ao atingir ERROR, IDs/usuários independentes, uma linha vigente e outra deletada, ambiguidade persistente com duas não deletadas, guardas para estados não PENDING e preservação do ramo de recusa explícita. Os testes existentes foram adaptados aos métodos de leitura corrigidos.

A execução focal em JDK 23 e release 17 passou no checkout preparado de `origin/dev`: 34 testes, nenhuma falha, erro ou teste ignorado, em 4 min 19 s. Foram 8 de `TransactionProcessorServiceTest`, 9 novos de `TransactionQueueRecoveryTest`, 13 de `TransactionServiceTest` e 4 de `TransactionWorkerServiceTest`. Maven usou heap de 256 MiB e um fork de testes de 512 MiB; nenhuma dependência Docker ou banco real foi usada. Comando focal:

```powershell
mvn -Dtest=TransactionQueueRecoveryTest,TransactionProcessorServiceTest,TransactionWorkerServiceTest,TransactionServiceTest test
```

O port no checkout WIP do usuário também passou em execução combinada: 67 testes, nenhuma falha, erro ou teste ignorado, em 3 min 36 s, compilando 229 fontes de produção e 58 fontes de teste. Foram os 34 testes de fila mais 24 testes existentes de IA e 9 de recorder/estoque financeiro. Os 13 arquivos anteriores de IA e integração financeira mantiveram o mesmo SHA256 após o port. Nenhum teste adicional ficou pendente para esta correção.

Checkout preparado: `%TEMP%/frigus-core-subscription-ready-20261101`. Nenhum commit, push, deploy, processamento de pagamento ou alteração de banco foi executado pelo agente responsável.
