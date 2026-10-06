# To-do — capacidades necessárias na core-api

Este documento reúne capacidades das telas que ainda precisam de contrato ou implementação na API principal.

## Bloqueadores funcionais

- Dashboard doméstico: resumo por usuário/grupo, itens próximos da validade, consumo semanal, distribuição por local e compras pendentes. Não existe `GET /stocks/my-summary` na core-api; a antiga tentativa do BFF apontava para um endpoint inexistente.
- Lista de compras: as entidades existem no banco, mas não há controller/DTO/service público para criar, editar, marcar comprado, limpar concluídos ou sugerir itens.
- Alertas e notificações: validade próxima, estoque baixo/zerado, itens adicionados à lista, lembretes de compras, entrada de membros e cliques de anúncios têm notificações e caixa de leitura. Preferências de alerta e resumo semanal ainda não estão disponíveis.
- Configurações domésticas: faltam preferências de lembrete, resumo semanal e tema.
- Receitas: as entidades existem e há IA por estoque, mas não há CRUD/listagem/detalhe de receitas, favoritos, ingredientes disponíveis nem sugestões gerais para as telas.
- Recuperação de senha: a tela pede envio de e-mail/link de redefinição; a core-api só tem troca de senha para usuário autenticado e reset administrativo.

## Lacunas de contrato para as telas existentes

- Estoque: `StockProductResponseDto` traz apenas `productId`; para renderizar nome, unidade, local e preço a tela precisa de produto expandido ou um endpoint de consulta em lote. Não há imagem, marca, lote, nem operação de remoção de estoque/produto.
- Consumo: a tela doméstica pede consumo parcial com observação. A core-api oferece movimentação (`IN`, `OUT`, `ADJUSTMENT`), mas não aceita observação; `DiscardCreateRequestDto` não recebe quantidade. Definir qual dos dois representa consumo e completar o contrato.
- Família: o convite visual usa nome, e-mail e permissão de edição/visualização. A core-api aceita somente `userId` de uma conta já existente e não possui papéis/permissões por membro nem convite por e-mail.
- Chat: há REST e WebSocket, mas ainda faltam anexos/reações pesquisáveis que a interface sugere; confirmar escopo antes de criar.
- Comercial: despesas, relatório mensal/exportação, compras concluídas com total/fornecedor e gestão de funcionários não têm endpoints/modelos correspondentes.
- Planos: a tela apresenta benefícios e seleção; há catálogo e checkout, mas é preciso definir a operação que vincula a escolha ao fluxo de pagamento e o formato de apresentação dos benefícios.

## Decisões necessárias

1. Definir o grupo/estoque ativo do usuário para evitar enviar `groupId` em toda chamada de estoque.
2. Definir se produtos do catálogo podem ser criados/geridos por usuário comercial, já que hoje o CRUD é administrativo.
3. Priorizar lista de compras, alertas e resumo de estoque para liberar o fluxo doméstico de ponta a ponta.
