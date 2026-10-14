# To-do — capacidades necessárias na core-api

Revisão do estado atual dos controllers, DTOs, serviços, modelos e configuração de segurança em 07/10/2026. Este documento registra lacunas de produto/contrato; endpoints já implementados aparecem como contexto para evitar reabrir trabalho concluído.

## Bloqueadores funcionais

- **Dashboard doméstico:** ainda não existe `GET /stocks/my-summary` nem operação equivalente que agregue, por usuário/grupo, itens próximos da validade, consumo semanal, distribuição por local e compras pendentes. Estoques são listados por `groupId` (`GET /stocks`).
- **Lista de compras — cobertura parcial implementada:** `ShoppingListController` e serviços já permitem criar, filtrar/listar, detalhar, atualizar, excluir, fechar/cancelar/reabrir listas; gerar lista por baixo estoque; adicionar itens unitários/em lote, paginar itens, atualizar quantidade/status, marcar comprado e remover. O DTO calcula `estimatedTotal` por item e por lista usando o preço unitário atual do produto. Não há registro do preço efetivamente pago, fornecedor ou total final da compra; confirmar se a tela comercial/doméstica precisa desses dados ou de compras concluídas separadas.
- **Alertas e notificações — cobertura parcial implementada:** há listagem paginada e marcação individual como lida (`/notifications`), registro de marcos de cliques de anúncios e scheduler para validade próxima e lembrete de lista aberta com itens pendentes. Não há contrato para preferências por usuário/grupo, resumo semanal ou configuração dos tipos/frequência de alerta. Confirmar quais outros eventos e canais as telas precisam receber.
- **Configurações domésticas:** faltam preferências de lembrete, resumo semanal e tema.
- **Receitas — cobertura parcial implementada:** `/recipes` já oferece criação, listagem paginada, detalhe, atualização e exclusão (rotas herdadas de `BaseController`); `/ingredients` oferece gestão/listagem de ingredientes e consulta por receita/produto; IA expõe chat e sugestões baseadas no estoque. Ainda faltam favoritos, consulta estruturada de ingredientes disponíveis no estoque e sugestões gerais/não dependentes do chat, se exigidas pelas telas.
- **Recuperação de senha:** autenticação expõe cadastro/login/refresh/logout e o perfil permite troca autenticada de senha; `/user/{id}/password` é reset administrativo. Falta fluxo público seguro de solicitação e conclusão por link/token de redefinição.

## Lacunas de contrato para as telas existentes

- **Consumo:** `POST /stock-products/{id}/movements` aceita tipo e quantidade (`IN`, `OUT`, `ADJUSTMENT`), mas não observação. Descarte já aceita quantidade e motivo (`POST /discard`). Definir se consumo parcial deve ser movimento `OUT` com observação, descarte, ou fluxo separado; completar o DTO escolhido.
- **Família/grupo:** gestão de grupos/membros existe, mas adicionar membro recebe apenas `userId` de uma conta existente (`POST /groups/{id}/members`). Não há convite por e-mail/nome nem papel de edição/visualização por membro; o vínculo atual não modela permissões por membro.
- **Chat:** REST/WebSocket e leitura de mensagens estão implementados. Há modelo de anexos, mas o contrato de envio não oferece anexos; não há reações nem busca de mensagens. Confirmar quais desses recursos a interface realmente requer.
- **Comercial:** não foram encontrados endpoints/modelos dedicados a despesas, relatório mensal/exportação, compras concluídas com total/fornecedor ou gestão de funcionários. A lista de compras atual cobre itens e status, sem esses dados financeiros.
- **Planos e pagamento:** catálogo, limites, assinatura, checkout e operações administrativas existem. O DTO público do plano expõe descrição, preço, intervalo de cobrança e limites, sem uma lista estruturada de benefícios. Definir como a escolha de plano da tela deve iniciar/associar o checkout e se a descrição atual basta para apresentar os benefícios.
- **Catálogo de produtos:** CRUD existe, mas criação/edição/exclusão são restritas a ADMIN. Decidir se usuário comercial precisa de gestão própria e, se sim, definir autorização e escopo do catálogo.
- **Estoque ativo:** chamadas de estoque exigem `groupId` e operações de itens dependem do estoque. Definir seleção/persistência de grupo e estoque ativo para evitar que cada chamada da interface tenha de resolver esse contexto separadamente.

## Decisões e prioridade

1. Priorizar resumo do estoque/dashboard, preferências de alertas e fluxo de lista de compras com os campos necessários às telas domésticas.
2. Definir o contrato de consumo com observação e o modelo de convite/permissões de membros.
3. Confirmar escopo de favoritos/sugestões de receitas, anexos/busca no chat e recuperação pública de senha.
4. Fechar o fluxo de seleção de plano/checkout e o escopo comercial (compras, relatórios, despesas, funcionários e catálogo).
