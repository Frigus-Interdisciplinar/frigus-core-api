# Money Saving — execução manual no Swagger

Rodada em 14–15/10/2026, na branch `feat/money-saving-core-api`, commit `ce072a1`. A API rodou em `localhost:18080` com PostgreSQL e Redis isolados em contêineres locais. O esquema `db/script.sql` e a migração `db/migrations/006_money_saving.sql` foram aplicados antes do início. Todos os dados abaixo são sintéticos. Senhas, cookies e tokens foram omitidos.

## Legenda e dados fixos

- **Swagger**: requisição feita em `http://localhost:18080/swagger-ui/index.html`, com cookie de `POST /auth/login`.
- **HTTP complementar**: requisição local com a mesma API e banco, após o controle do navegador travar. Não constitui evidência de interação pelo Swagger.
- Grupo `13127a54-c506-4aae-a9c7-d78262943e00`, fuso `America/Sao_Paulo`; estoques 1 (`MS Fridge`) e 2 (`MS Pantry`).
- Produtos: 1 `MS Leite`/`DAIRY` (catálogo R$ 99,00), 2 `MS Arroz`/`GRAIN` (catálogo R$ 88,00), 3 `MS Detergente`/`CLEANING` (catálogo R$ 50,00).
- Proprietário `4cf6fa14-f91d-4919-983a-e2a93f6d211b`, administrador `c8aa5587-591d-4b80-a5da-058d542e4380`, externo `b645013b-7ced-44c3-8715-6c7fa173c033`.
- `D=2026-10-14` em São Paulo no momento dos eventos, `M=2026-10-01`, `P=2026-09-15`. Período principal `[2026-10-01, 2026-11-01)`.

## Preparação pelo Swagger

| Chamada e corpo/parâmetros | Status | Evidência retornada |
|---|---:|---|
| `POST /auth/register` × 3, contas sintéticas owner/admin/outsider | 201 × 3 | IDs dos usuários acima. A data de nascimento exigiu formato `dd/MM/yyyy`. |
| `POST /auth/login` com conta admin | 200 | Cookie usado no `POST /products`. |
| `POST /products` `{"name":"MS Leite","category":"DAIRY","storagePlace":"FRIDGE","unitPrice":99,"unitOfMeasure":"UNIT"}` | 201 | ID 1. |
| `POST /products` `{"name":"MS Arroz","category":"GRAIN","storagePlace":"PANTRY","unitPrice":88,"unitOfMeasure":"UNIT"}` | 201 | ID 2. |
| `POST /auth/login` com conta owner | 200 | Cookie usado nas chamadas do grupo. |
| `POST /groups` `{"name":"MS Test Group","timeZone":"America/Sao_Paulo"}` | 201 | ID do grupo acima; proprietário e fuso corretos. |
| `POST /stocks` `{"groupId":"13127a54-c506-4aae-a9c7-d78262943e00","name":"MS Fridge"}` | 201 | ID 1. |
| `POST /stocks` `{"groupId":"13127a54-c506-4aae-a9c7-d78262943e00","name":"MS Pantry"}` | 201 | ID 2. |
| `GET /groups/{groupId}/money-saving/summary?from=2026-10-01&to=2026-11-01` | 200 | Gasto/economia/perda zero, `hasFinancialData=false`, seis meses de maio a outubro zerados. |

O administrador recebeu papel `ADMIN` e o proprietário uma assinatura PLUS diretamente **no banco descartável** para permitir a preparação dos dados. Isso não foi feito pelo Swagger.
Antes dos três cadastros válidos, o corpo inicial gerado pelo Swagger para a data de nascimento retornou 400; o contrato aceitou `dd/MM/yyyy`. Os logins de renovação do cookie durante a rodada retornaram 200. Seus corpos continham apenas email sintético e senha de teste, omitida deste relatório.

## Compras, consumo e descarte pelo Swagger

Todos os `POST /groups/{groupId}/food-purchases` usaram o ID de grupo acima, `source=USER_CONFIRMED` e chaves `Idempotency-Key` distintas, salvo os testes de repetição.

| Chamada e corpo/parâmetros | Status | Evidência retornada |
|---|---:|---|
| Compra A, chave `ms-a-20261014`: `{"purchaseDate":"2026-10-14","source":"USER_CONFIRMED","receiptReference":"ms-a-20261014","lines":[{"stockId":1,"productId":1,"expireDate":"2026-10-15","quantity":4,"unitPrice":3,"category":"DAIRY"}]}` | 201 | Compra `e478a844-2091-4295-b602-e273ed8eb8c2`, linha 1, item de estoque 1, versão 1, R$ 12,00. |
| Repetição da compra A com a mesma chave e corpo | 201 | Mesmo ID da compra A; saldo e gasto não duplicados. |
| Mesma chave da A, corpo igual exceto `quantity:3` | 409 | `Chave de idempotência usada com outra compra`. |
| Compra B, chave `ms-b-20261014`: `{"purchaseDate":"2026-10-14","source":"USER_CONFIRMED","receiptReference":"ms-b-20261014","lines":[{"stockId":1,"productId":1,"expireDate":"2026-10-15","quantity":2,"unitPrice":5,"category":"DAIRY"}]}` | 201 | Compra `221f5d17-f0ff-4db5-ae2d-bbc1f7e06f4d`, linha 2, **mesmo item de estoque 1**, R$ 10,00. |
| Compra C, chave `ms-c-20261014`: `{"purchaseDate":"2026-09-15","source":"USER_CONFIRMED","receiptReference":"ms-c-20261014","lines":[{"stockId":2,"productId":2,"expireDate":"2026-10-13","quantity":2,"unitPrice":7,"category":"GRAIN"}]}` | 201 | Compra `332b913c-528b-45d0-a1e7-eb973138615e`, linha 3, item 2, R$ 14,00. |
| Resumo do mês após A/B/C | 200 | Gasto R$ 22,00; anterior R$ 14,00; variação +57,1%; categoria DAIRY R$ 22,00. |
| `POST /stock-products/1/movements` `{"movementType":"OUT","quantity":5,"reasonCode":"CONSUMED"}` | 201 | Movimento 1, saldo 1. FIFO: 4 × R$ 3,00 + 1 × R$ 5,00 = R$ 17,00. |
| `POST /discard` `{"stockProductId":2,"quantity":1,"reason":"Vencido no estoque","reasonCode":"EXPIRED"}` | 201 | Descarte 1. Resumo: economia R$ 17,00 e perda R$ 7,00. |
| `DELETE /discard/1` | 200 | Cancelamento aceito. |
| `GET /discard/1` | 404 | Descarte cancelado oculto na leitura por ID. |
| Repetição do `POST /discard` acima | 201 | Descarte 2; perda R$ 7,00. |
| `PUT /groups/{groupId}/food-purchases/{A}` com `{"purchaseDate":"2026-10-14","source":"USER_CONFIRMED","receiptReference":"ms-a-20261014","version":1,"lines":[{"id":1,"quantity":4,"unitPrice":4,"category":"DAIRY"}]}` | 200 | Versão 2, A R$ 16,00. Resumo: gasto R$ 26,00; economia R$ 21,00; perda R$ 7,00; anterior R$ 14,00; +85,7%. |
| Mesmo PUT com `version:1` novamente | 409 | `Versão da compra desatualizada`. |
| Mesmo PUT com `version:2` e `quantity:3` | 409 | `Quantidade menor que as unidades já alocadas`. |
| `DELETE /groups/{groupId}/food-purchases/{A}` e `{B}` | 409 cada | `Compra com saídas vinculadas não pode ser excluída`. A primeira tentativa de A havia dado 403 após expiração do cookie; novo login e repetição produziram 409. |
| Compra temporária, chave `ms-d-20261014`: `{"purchaseDate":"2026-10-14","source":"USER_CONFIRMED","receiptReference":"ms-d-20261014","lines":[{"stockId":2,"productId":2,"expireDate":"2026-10-16","quantity":1,"unitPrice":2,"category":"GRAIN"}]}` | 201 | Compra `41a7d177-0f4a-41b0-91a4-5b9696e0afce`, linha 4, item 3, R$ 2,00. |
| `DELETE /groups/{groupId}/food-purchases/{temporária}` | 204 | Compra sem saída removida do gasto; estoque estornado. |

Uma primeira tentativa de PUT da compra A enviou `stockId`, `productId` e `expireDate` na linha e recebeu `400` (`Produto, estoque e validade não podem mudar nesta correção`). O PUT válido acima omitiu esses campos.

## Estoque legado e relatórios pelo Swagger

| Chamada e corpo/parâmetros | Status | Evidência retornada |
|---|---:|---|
| `POST /stock-products` `{"productId":1,"stockId":2,"quantity":2,"minimalQuantity":0,"purchaseUnitPrice":99,"expireDate":"2026-10-17"}` | 201 | Item legado 4, saldo 2. O preço de R$ 99,00 não é compra confirmada. |
| `POST /stock-products/4/movements` `{"movementType":"IN","quantity":1}` | 201 | Movimento 2, saldo 3. |
| Mesmo endpoint `{"movementType":"OUT","quantity":1}` | 201 | Movimento 3, saldo 2; não classificado como consumo. |
| Mesmo endpoint `{"movementType":"ADJUSTMENT","quantity":1}` | 201 | Movimento 4, saldo absoluto 1. |
| Mesmo endpoint `{"movementType":"OUT","quantity":1,"reasonCode":"CONSUMED"}` | 201 | Movimento 5, saldo 0. Evento `E:6`: `amount=null`, `verifiedValue=false`, `includedInMetric=false`. Economia monetária continuou R$ 21,00. |
| Mesmo endpoint `{"movementType":"IN","quantity":1,"reasonCode":"CONSUMED"}` | 400 | CONSUMED só é aceito para OUT. |
| `POST /stock-products/1/movements` `{"movementType":"OUT","quantity":2,"reasonCode":"CONSUMED"}` | 400 | Saldo insuficiente; item 1 tinha saldo 1. |
| `GET /groups/{groupId}/money-saving/events?from=2026-10-01&to=2026-11-01&page=0,1,2&size=2` | 200 × 3 | `totalElements=5` em todas; 2, 2 e 1 linhas. Eventos: duas compras (R$ 16,00 e R$ 10,00), consumo verificado R$ 21,00, descarte R$ 7,00 e consumo legado sem valor. |
| Mesmo GET com `kind=PURCHASE`, `CONSUMED`, `DISCARDED` | 200 × 3 | 2 compras, 2 consumos e 1 descarte. Só um consumo tem valor verificável. |
| Mesmo GET com `category=DAIRY` e `GRAIN` | 200 × 2 | 4 eventos DAIRY e 1 GRAIN. |
| Mesmo GET com `from=2026-10-01&to=2026-10-14` | 200 | Nenhum evento: D é excluído por `to`. |
| Mesmo GET com `from=2026-10-14&to=2026-10-15` | 200 | Os cinco eventos de D: `from` inclusivo. |
| Mesmo GET com `from=to=2026-10-14` | 400 | Intervalo inválido. |

O resumo final no Swagger retornou `spending.amount=26`, `saving.amount=21`, `saving.actions=2` (inclui o consumo legado confirmado), `loss.amount=7`, `previousSpending=14`, `spendingChangePercent=85.7`. As cinco categorias vieram na resposta; apenas DAIRY teve gasto/economia e GRAIN teve perda. A série de seis meses trouxe setembro R$ 14,00 e outubro R$ 26,00/R$ 21,00/R$ 7,00, com os demais meses zerados. `GET /stock-products/1` e `/2` confirmaram saldos 1 e 1; `GET /discard/2` e a lista própria mostraram só o descarte ativo.

## Erros e isolamento pelo Swagger

| Chamada e corpo/parâmetros | Status | Evidência retornada |
|---|---:|---|
| `PUT /groups/{id}` `{"name":"MS Test Group","timeZone":"America/Sao_Paulo"}` | 200 | Fuso aceito. |
| Mesmo PUT com `timeZone:"Mars/Olympus"` | 400 | Fuso IANA inválido. |
| `POST /groups/{groupId}/food-purchases`, chave nova `ms-duplicate-note`, mesma `receiptReference` de A e linha DAIRY de 1 × R$ 3,00 | 409 | Nota já registrada no grupo. |
| `POST /products` pelo admin `{"name":"MS Detergente","category":"CLEANING","storagePlace":"PANTRY","unitPrice":50,"unitOfMeasure":"UNIT"}` | 201 | Produto 3. |
| `POST /groups/{groupId}/food-purchases`, chave `ms-nonfood`, linha com `stockId:2,productId:3,expireDate:"2026-10-16",quantity:1,unitPrice:5,category:"CLEANING"` | 400 | Categoria alimentar inválida. |
| `GET /money-saving/events` no período principal com `category=CLEANING` ou `kind=INVALID` | 400 cada | Categoria e tipo inválidos. |
| Mesmo GET com `page=-1` ou `size=0` | 400 cada | Paginação inválida. |
| Login como conta externa e repetição de resumo, eventos, detalhe de A, PUT de A (mesmo corpo válido), movimento OUT/CONSUMED do item 1, POST de descarte, GET `/discard/2` e DELETE `/discard/2` | 403 em cada | `Usuário não pertence ao grupo informado`. Nenhuma escrita externa foi aplicada. |
| `GET /discard?page=0&size=20` pela conta externa | **200** | `content:[]`; a lista filtra pelos grupos do usuário. O roteiro previa 403. |
| `GET /discard` com o exemplo padrão do Swagger `{"page":0,"size":1,"sort":["string"]}` | **500** | O Swagger gera `sort=["string"]`; Spring Data rejeita a expressão de ordenação. `sort:[]` também vira `sort=[]` e causa 500. Omitir a propriedade `sort` do objeto de paginação retorna 200. |

## Verificação complementar por HTTP local

O controle do navegador ficou indisponível antes de abrir mais operações. Estas chamadas foram feitas com cookie da conta proprietária na mesma API e banco isolados; não devem ser contadas como prova do Swagger.

| Chamada e parâmetros/corpo | Status | Resultado |
|---|---:|---|
| `GET /groups/{id}`, `/stocks/2`, `/products/1`, `/products/2` | 200 cada | IDs e fuso preservados. |
| `GET /products?page=0&size=20` | 200 | 3 produtos (inclui CLEANING de validação). |
| `GET /stock-products?stockId=1&page=0&size=20` e `stockId=2` | 200 cada | 1 e 3 itens, respectivamente. |
| `GET /stock-products/4` | 200 | Saldo legado 0. |
| `GET /stock-products/1/movements?page=0&size=20` e item 4 | 200 cada | 1 e 4 movimentos. |
| `GET /groups/{groupId}/food-purchases/{B}` | 200 | Versão 1, total R$ 10,00. |
| `DELETE /discard/2`; em seguida resumo do mês | 200; 200 | Perda caiu a R$ 0,00. |
| `POST /discard` `{"stockProductId":2,"quantity":1,"reason":"Vencido no estoque","reasonCode":"EXPIRED"}`; em seguida resumo | 201; 200 | Descarte ativo final **ID 3**; perda voltou a R$ 7,00. |

## Resultado e limites

Os cálculos centrais, FIFO, idempotência, correção, filtros, paginação financeira e isolamento por ID bateram com o roteiro. **A rodada não passa integralmente como escrita**: a listagem global de descarte da conta externa retorna 200 vazio, e o exemplo padrão de paginação de `GET /discard` no Swagger provoca 500. Para executá-la manualmente hoje, use `{"page":0,"size":20}` sem `sort` no campo `pageable`.

O navegador travou antes de repetir todos os GET auxiliares pelo Swagger; esses casos estão separados acima. Concorrência, rollback e eventos perto da meia-noite não foram provados nesta rodada manual e seguem dependentes de testes automatizados com relógio e banco controlados. O banco local permanece isolado e contém somente esta fixture sintética.
