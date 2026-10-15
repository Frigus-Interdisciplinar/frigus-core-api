# Complemento da rodada Money Saving: listagem de descartes

Este complemento registra a correção de `GET /discard` após a [rodada de 14–15/10/2026](money-saving-swagger-2026-10-14.md). A rodada anterior foi preservada. Verificação em 15/10/2026, branch `feat/money-saving-core-api`, sobre `ce072a1`, com alterações locais ainda sem commit. A API corrigida foi carregada somente no contêiner isolado `frigus-ms-api-20261014`, com PostgreSQL e Redis isolados da rodada anterior. Nenhuma migração foi aplicada.

## Contrato publicado

`GET /v3/api-docs` retornou, para `GET /discard`, quatro parâmetros de *query* separados:

| Parâmetro | Obrigatório | Esquema e padrão |
|---|---|---|
| `groupId` | Sim | `string`, formato `uuid` |
| `page` | Não | inteiro, padrão `0`, mínimo `0` |
| `size` | Não | inteiro, padrão `20`, mínimo `1` |
| `sort` | Não | lista de strings, padrão `date,DESC` e `id,DESC` |

Não há parâmetro objeto `pageable`. Os dois valores iniciais de `sort` têm formato aceito pelo endpoint. A aba visual do Swagger não pôde ser anexada pelo navegador interno em duas tentativas; por isso a aparência do formulário e o clique em **Execute** não foram confirmados visualmente. As requisições equivalentes foram executadas por HTTP local na mesma instância.

## Requisições na API isolada

Foi criada uma conta sintética temporária. A primeira leitura ocorreu sem vínculo com o grupo `13127a54-c506-4aae-a9c7-d78262943e00`; depois a conta recebeu vínculo temporário para as leituras autorizadas. O vínculo e a conta foram removidos ao final. O banco de testes continha um descarte ativo do grupo e a fixture financeira da rodada anterior.

| Chamada | Status | Resultado |
|---|---:|---|
| `GET /discard?groupId={grupo}` sem vínculo | 403 | Acesso negado ao grupo. |
| `GET /discard?groupId={grupo}` com vínculo e valores padrão | 200 | `totalElements=1`, um descarte ativo. |
| Mesmo GET com `page=0&size=1&sort=date,desc` | 200 | Uma linha, `totalElements=1`. |
| Mesmo GET com `sort=[]` | 400 | Ordenação inválida. |
| Mesmo GET com `sort=["string"]` | 400 | Ordenação inválida. |
| Mesmo GET com `sort=unknown,desc` | 400 | Ordenação inválida. |
| `GET /discard?page=0&size=20` sem `groupId` | 400 | Parâmetro obrigatório ausente. |
| `GET /groups/{grupo}/money-saving/summary?from=2026-10-01&to=2026-11-01` | 200 | Gasto R$ 26,00; economia R$ 21,00; perda R$ 7,00, iguais à rodada anterior. |
| `GET /groups/{grupo}/money-saving/events?from=2026-10-01&to=2026-11-01&size=20` | 200 | Cinco eventos, igual à rodada anterior. |

## PostgreSQL de integração

`MoneySavingHttpIntegrationTest` usa Testcontainers com PostgreSQL 16 e Redis 7. A fixture agora define o driver PostgreSQL explicitamente; antes disso, o `application.properties` de teste fixava H2 e impedia essa classe de conectar ao contêiner. Os cenários cobrem grupo autorizado com descartes ativos, grupo vazio, conta externa (`403`), ausência de `groupId` (`400`), grupo inativo (`404`), descarte cancelado, páginas estáveis por data e ID, ordenação válida e inválida. O teste anterior de compras, consumo, resumo e eventos também integra a mesma execução.

Comando final: `mvn -q -Dtest=MoneySavingHttpIntegrationTest test` (Maven local, JDK 17). Resultado: **2 testes, 0 falhas, 0 erros**. Os cinco campos aceitos (`id`, `date`, `quantity`, `reason`, `reasonCode`) foram consultados em `asc` e `desc`; `[]`, `["string"]`, campo desconhecido e direção inválida receberam `400`.

O contrato agora exige `groupId`; clientes que chamavam a listagem sem esse parâmetro receberão `400`. Não houve alteração de outros endpoints paginados nem migração de banco.
