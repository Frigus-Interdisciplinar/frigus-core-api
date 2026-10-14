# Money Saving — implantação da core API

1. Aplicar `db/migrations/006_money_saving.sql` antes de publicar o binário. A migração pode ser reaplicada; ela cria tabelas e colunas sem reclassificar registros antigos e remove o gatilho que duplicava movimentações de estoque.
2. Confirmar que `trg_stock_movements_apply` não existe e que `stock_movements`, `discard`, `food_purchases`, `money_supplies`, `money_exits` e `money_allocations` estão disponíveis. Publicar a API em seguida.
3. Para voltar ao binário anterior, manter as tabelas e colunas novas. O serviço anterior também altera o saldo diretamente; não recriar o gatilho. Dados financeiros já coletados permanecem preservados para a próxima publicação.

Itens existentes entram no livro de custos como saldo de preço desconhecido no primeiro uso após a migração. Movimentos e descartes antigos não são convertidos em economia ou perda. `purchase_unit_price` e preço de catálogo continuam fora dos relatórios.

As consultas usam datas civis `[from,to)` e o fuso IANA do grupo; os grupos existentes recebem `America/Sao_Paulo`. Corrigir uma compra muda relatórios anteriores e cria uma revisão em `food_purchase_revisions`.
