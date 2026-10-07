# TODO — lacunas da core-api para o web

Este arquivo registra somente capacidades de backend que ainda não têm contrato
na core-api. As telas web que ainda não existem ou não estão ligadas aos services
estão acompanhadas em `frigus-bff/docs/core-api-todo.md` e
`frigus-react-web/docs/services.md`.

## O que já existe na core-api

Não recriar estas capacidades como se estivessem ausentes:

- Dashboard e contexto: `GET /stocks/my-summary` e `GET /stocks/active-context`.
- Compras: `/shopping-lists` inclui listas, itens, sugestões, conclusão e limpeza
  de itens comprados.
- Notificações e preferências: `/notifications` e `/profile/preferences`;
  preferências cobrem alertas de validade/estoque, lembretes, resumo semanal e tema.
- Receitas: `/recipes` inclui catálogo, detalhe, favoritos, disponibilidade e
  sugestões; `/ingredients` consulta ingredientes.
- Família: grupos, convites por e-mail, aceite e papéis de membro.
- Comercial: despesas, relatório mensal/CSV, funcionários e convites.
- Planos e cobrança: catálogo, limites, seleção via checkout e transações.
- Recuperação de senha: solicitar código, validar e redefinir senha com código
  temporário enviado por e-mail.

## Lacunas reais

- **Consumo:** movimentações já aceitam `OUT`, quantidade e observação; descartes
  aceitam quantidade e motivo. Não há lacuna conhecida para o consumo parcial
  descrito nas telas atuais.
- **Chat:** não há busca de mensagens nem reações. Mensagens do tipo imagem podem
  circular como conteúdo, mas não há contrato de upload/armazenamento de anexos.
- **Grupo ativo persistente:** o contexto de estoque pode ser resolvido por
  requisição ou `groupId`, mas não existe preferência persistida de grupo/estoque
  ativo por usuário. Só implementar se o web precisar reabrir a última seleção.

## Fora da core-api

- Termos de uso e política de privacidade são conteúdo do produto/web, sem
  necessidade de endpoint da API.
- Integração das telas planejadas com services e contratos BFF está descrita no
  TODO do BFF; não é uma lacuna da core-api.
