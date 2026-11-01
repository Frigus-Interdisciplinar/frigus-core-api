# Preço de aquisição na criação de lote

`StockProductCreateRequestDto.purchaseUnitPrice` já recebia um preço opcional e `StockProductMapper` o conservava no lote. Isso, sozinho, não alimentava o MoneySaving: o cálculo usa `food_purchase_lines.unit_price`, supplies e alocações do ledger, não o preço atual do catálogo nem apenas o snapshot do lote.

`StockProductService.create` agora injeta obrigatoriamente `StockPurchaseRecorder`. A mesma transação bloqueia primeiro a linha do grupo, como `FoodPurchaseService`, persiste e faz flush do lote, e registra a aquisição das unidades já criadas. Não chama `FoodPurchaseService.create`, pois esse fluxo somaria a quantidade novamente.

Para lote com quantidade positiva, preço de aquisição informado e categoria alimentar suportada (`FRUIT`, `VEGETABLE`, `MEAT`, `DAIRY`, `GRAIN`, `BEVERAGE`), o recorder grava uma compra `USER_CONFIRMED`, uma linha apontando para o próprio lote e um supply com `unknown_cost=false`. A data é o dia atual no fuso do grupo. A chave `stock-product/{lotId}` e o hash dos campos da aquisição impedem repetir esse registro para o mesmo lote. Repetição equivalente em outro dia reutiliza o registro original. Conflitos ou compras excluídas não criam outra aquisição.

Nenhuma operação do recorder incrementa o saldo. Também não chama `ensureOpening` para essas unidades: elas já possuem um supply de custo conhecido. `ledger.rebuild` mantém o mesmo mecanismo FIFO usado pelas compras existentes. Se qualquer gravação ou reconstrução falhar, lote, compra, linha e supply são revertidos juntos. As duas entradas públicas do recorder exigem transação existente (`MANDATORY`).

Produtos não alimentares conservam o preço do lote sem criar gasto alimentar. Lotes sem preço ou sem unidades não inventam uma compra. O preço do catálogo nunca é usado como substituto pelo recorder. A alteração é aplicada somente à criação de novos lotes; não faz backfill, não reinterpreta edições de snapshot como correção de compra e não modifica endpoints, permissões ou a restrição comercial do MoneySaving.

Os testes `StockPurchaseRecorderTest` usam JDBC/H2 e proxies transacionais reais para conferir o saldo sem duplicação, fuso da compra, custo confirmado nas alocações, idempotência, rollback conjunto e exigência de transação. `StockProductServiceTest` também confere autorização e ordem de lock/flush/registro. O schema usa as tabelas da migração MoneySaving já existente em `db/migrations/006_money_saving.sql`.
