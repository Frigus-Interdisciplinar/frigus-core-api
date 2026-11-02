# Mapa de endpoints - linha de base

Extraido dos controllers e rotas herdadas de BaseController. A presenca de testes unitarios nao implica cobertura HTTP. As lacunas corrigidas e restantes estao no relatorio.

| Metodo | Rota | Regra principal | Autenticacao | Testes existentes | Lacunas iniciais | Prioridade |
|---|---|---|---|---|---|---|
| POST | /ai/recipes/chat | Acesso ao estoque; sugestao e fallback do fornecedor | JWT; escopo no service | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| GET | /ai/recipes/suggestions/stock/{stockId} | Acesso ao estoque; sugestao e fallback do fornecedor | JWT; escopo no service | Controller unitario + service | Contrato HTTP nao validado na linha de base | P2 |
| POST | /auth/register | Cadastro >=16 anos, hash Argon2, login e rotacao Redis | Publico | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| POST | /auth/login | Cadastro >=16 anos, hash Argon2, login e rotacao Redis | Publico | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| POST | /auth/refresh | Cadastro >=16 anos, hash Argon2, login e rotacao Redis | Publico | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| POST | /auth/logout | Cadastro >=16 anos, hash Argon2, login e rotacao Redis | Publico | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| GET | /conversations | Participante ativo e grupo em comum; conversas e historico | JWT; escopo no service | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| GET | /conversations/{id} | Participante ativo e grupo em comum; conversas e historico | JWT; escopo no service | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| POST | /conversations/group | Participante ativo e grupo em comum; conversas e historico | JWT; escopo no service | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| POST | /conversations/private | Participante ativo e grupo em comum; conversas e historico | JWT; escopo no service | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| POST | /conversations/{id}/participants | Participante ativo e grupo em comum; conversas e historico | JWT; escopo no service | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| DELETE | /conversations/{id}/leave | Participante ativo e grupo em comum; conversas e historico | JWT; escopo no service | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| GET | /conversations/{id}/messages | Participante ativo e grupo em comum; conversas e historico | JWT; escopo no service | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| POST | /conversations/{id}/messages | Participante ativo e grupo em comum; conversas e historico | JWT; escopo no service | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| POST | /conversations/{id}/messages/{messageId}/read | Participante ativo e grupo em comum; conversas e historico | JWT; escopo no service | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| GET | /discard | Registrar motivo vinculado a item existente | JWT | Service unitario | Contrato HTTP nao validado na linha de base; escopo por grupo ausente no service | P2 |
| POST | /discard | Registrar motivo vinculado a item existente | JWT | Service unitario | Contrato HTTP nao validado na linha de base; escopo por grupo ausente no service | P1 |
| GET | /discard/{id} | Registrar motivo vinculado a item existente | JWT | Service unitario | Rota herdada: contrato e autorizacao sem teste HTTP | P2 |
| DELETE | /discard/{id} | Registrar motivo vinculado a item existente | JWT | Service unitario | Rota herdada: contrato e autorizacao sem teste HTTP | P1 |
| GET | /groups | Um grupo ativo por usuario/dono; limites do plano; transferencia de dono e chat | JWT; escopo no service | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| GET | /groups/page | Um grupo ativo por usuario/dono; limites do plano; transferencia de dono e chat | JWT; escopo no service | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| GET | /groups/{id} | Um grupo ativo por usuario/dono; limites do plano; transferencia de dono e chat | JWT; escopo no service | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| POST | /groups | Um grupo ativo por usuario/dono; limites do plano; transferencia de dono e chat | JWT; escopo no service | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| PUT | /groups/{id} | Um grupo ativo por usuario/dono; limites do plano; transferencia de dono e chat | JWT; escopo no service | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| DELETE | /groups/{id} | Um grupo ativo por usuario/dono; limites do plano; transferencia de dono e chat | JWT; escopo no service | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| POST | /groups/{id}/members | Um grupo ativo por usuario/dono; limites do plano; transferencia de dono e chat | JWT; escopo no service | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| DELETE | /groups/{id}/members/{userId} | Um grupo ativo por usuario/dono; limites do plano; transferencia de dono e chat | JWT; escopo no service | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| DELETE | /groups/{id}/leave | Um grupo ativo por usuario/dono; limites do plano; transferencia de dono e chat | JWT; escopo no service | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| GET | /ingredients | Receita/produto existentes; ingrediente unico; unidade padrao | JWT | Controller unitario + service | Contrato HTTP nao validado na linha de base | P2 |
| GET | /ingredients/{id} | Receita/produto existentes; ingrediente unico; unidade padrao | JWT | Controller unitario + service | Contrato HTTP nao validado na linha de base | P2 |
| POST | /ingredients | Receita/produto existentes; ingrediente unico; unidade padrao | JWT | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| PUT | /ingredients/{id} | Receita/produto existentes; ingrediente unico; unidade padrao | JWT | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| GET | /ingredients/recipe/{recipeId} | Receita/produto existentes; ingrediente unico; unidade padrao | JWT | Controller unitario + service | Contrato HTTP nao validado na linha de base | P2 |
| GET | /ingredients/product/{productId} | Receita/produto existentes; ingrediente unico; unidade padrao | JWT | Controller unitario + service | Contrato HTTP nao validado na linha de base | P2 |
| DELETE | /ingredients/{id} | Receita/produto existentes; ingrediente unico; unidade padrao | JWT | Controller unitario + service | Rota herdada: contrato e autorizacao sem teste HTTP | P1 |
| GET | /plans | Planos ativos, limites por plano; mutacoes/admin restritos | Publico | Controller unitario + service | Contrato HTTP nao validado na linha de base | P2 |
| GET | /plans/active | Planos ativos, limites por plano; mutacoes/admin restritos | Publico | Controller unitario + service | Contrato HTTP nao validado na linha de base | P2 |
| GET | /plans/{id} | Planos ativos, limites por plano; mutacoes/admin restritos | Publico | Controller unitario + service | Contrato HTTP nao validado na linha de base | P2 |
| GET | /plans/code/{planCode} | Planos ativos, limites por plano; mutacoes/admin restritos | Publico | Controller unitario + service | Contrato HTTP nao validado na linha de base | P2 |
| GET | /plans/{planCode}/limits | Planos ativos, limites por plano; mutacoes/admin restritos | JWT | Controller unitario + service | Contrato HTTP nao validado na linha de base | P2 |
| GET | /plans/me/limits | Planos ativos, limites por plano; mutacoes/admin restritos | JWT | Controller unitario + service | Contrato HTTP nao validado na linha de base | P2 |
| POST | /plans | Planos ativos, limites por plano; mutacoes/admin restritos | ADMIN | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| PUT | /plans/{id} | Planos ativos, limites por plano; mutacoes/admin restritos | ADMIN | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| DELETE | /plans/{id} | Planos ativos, limites por plano; mutacoes/admin restritos | ADMIN | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| GET | /plans/admin/all | Planos ativos, limites por plano; mutacoes/admin restritos | ADMIN | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| GET | /products | Catalogo global; nome unico sem diferenca de caixa; impedir exclusao referenciada | Publico | Controller unitario + service | Contrato HTTP nao validado na linha de base | P2 |
| POST | /products | Catalogo global; nome unico sem diferenca de caixa; impedir exclusao referenciada | ADMIN | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| PUT | /products/{id} | Catalogo global; nome unico sem diferenca de caixa; impedir exclusao referenciada | ADMIN | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| DELETE | /products/{id} | Catalogo global; nome unico sem diferenca de caixa; impedir exclusao referenciada | ADMIN | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| GET | /products/{id} | Catalogo global; nome unico sem diferenca de caixa; impedir exclusao referenciada | Publico | Controller unitario + service | Rota herdada: contrato e autorizacao sem teste HTTP | P2 |
| GET | /profile | Dados e senha do usuario autenticado | JWT | Controller unitario + service | Contrato HTTP nao validado na linha de base | P2 |
| PUT | /profile | Dados e senha do usuario autenticado | JWT | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| PATCH | /profile | Dados e senha do usuario autenticado | JWT | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| PATCH | /profile/password | Dados e senha do usuario autenticado | JWT | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| DELETE | /profile | Dados e senha do usuario autenticado | JWT | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| GET | /stocks | Acesso ao grupo de origem e destino | JWT; escopo no service | Sem teste dedicado | Contrato HTTP nao validado na linha de base | P1 |
| POST | /stocks | Acesso ao grupo de origem e destino | JWT; escopo no service | Sem teste dedicado | Contrato HTTP nao validado na linha de base | P1 |
| PUT | /stocks/{id} | Acesso ao grupo de origem e destino | JWT; escopo no service | Sem teste dedicado | Contrato HTTP nao validado na linha de base | P1 |
| GET | /stocks/{id} | Acesso ao grupo de origem e destino | JWT; escopo no service | Sem teste dedicado | Rota herdada: contrato e autorizacao sem teste HTTP | P2 |
| DELETE | /stocks/{id} | Acesso ao grupo de origem e destino | JWT; escopo no service | Sem teste dedicado | Rota herdada: contrato e autorizacao sem teste HTTP | P1 |
| GET | /stock-products/{stockProductId}/movements | Acesso ao grupo; saldo nao negativo; auditoria e transacao | JWT; escopo no service | Service unitario | Contrato HTTP nao validado na linha de base; persistencia/trigger/rollback | P1 |
| POST | /stock-products/{stockProductId}/movements | Acesso ao grupo; saldo nao negativo; auditoria e transacao | JWT; escopo no service | Service unitario | Contrato HTTP nao validado na linha de base; persistencia/trigger/rollback | P1 |
| GET | /stock-products | Acesso ao grupo; lote unico produto/estoque/validade; status derivado | JWT; escopo no service | Service unitario | Contrato HTTP nao validado na linha de base | P1 |
| POST | /stock-products | Acesso ao grupo; lote unico produto/estoque/validade; status derivado | JWT; escopo no service | Service unitario | Contrato HTTP nao validado na linha de base | P1 |
| PUT | /stock-products/{id} | Acesso ao grupo; lote unico produto/estoque/validade; status derivado | JWT; escopo no service | Service unitario | Contrato HTTP nao validado na linha de base | P1 |
| GET | /stock-products/{id} | Acesso ao grupo; lote unico produto/estoque/validade; status derivado | JWT; escopo no service | Service unitario | Rota herdada: contrato e autorizacao sem teste HTTP | P2 |
| DELETE | /stock-products/{id} | Acesso ao grupo; lote unico produto/estoque/validade; status derivado | JWT; escopo no service | Service unitario | Rota herdada: contrato e autorizacao sem teste HTTP | P1 |
| GET | /subscriptions/me | Assinatura propria; cancelamento e reativacao; administracao | JWT; escopo no service | Controller unitario + service | Contrato HTTP nao validado na linha de base | P2 |
| POST | /subscriptions/me/cancel | Assinatura propria; cancelamento e reativacao; administracao | JWT; escopo no service | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| POST | /subscriptions/me/reactivate | Assinatura propria; cancelamento e reativacao; administracao | JWT; escopo no service | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| GET | /subscriptions | Assinatura propria; cancelamento e reativacao; administracao | ADMIN | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| GET | /subscriptions/{id} | Assinatura propria; cancelamento e reativacao; administracao | ADMIN | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| GET | /subscriptions/user/{userId} | Assinatura propria; cancelamento e reativacao; administracao | ADMIN | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| PATCH | /subscriptions/{id}/status | Assinatura propria; cancelamento e reativacao; administracao | ADMIN | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| POST | /subscriptions/{id}/cancel | Assinatura propria; cancelamento e reativacao; administracao | ADMIN | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| POST | /subscriptions/billing/run | Assinatura propria; cancelamento e reativacao; administracao | ADMIN | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| POST | /transactions/checkout | Titularidade, idempotencia, pagamento e cancelamento PENDING | JWT; escopo no service | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| GET | /transactions | Titularidade, idempotencia, pagamento e cancelamento PENDING | JWT; escopo no service | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| GET | /transactions/{transactionId} | Titularidade, idempotencia, pagamento e cancelamento PENDING | JWT; escopo no service | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| POST | /transactions/{transactionId}/cancel | Titularidade, idempotencia, pagamento e cancelamento PENDING | JWT; escopo no service | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| GET | /user | Administracao de usuarios | ADMIN | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| GET | /user/search | Administracao de usuarios | ADMIN | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| PATCH | /user/{id}/role | Administracao de usuarios | ADMIN | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| PATCH | /user/{id}/account-type | Administracao de usuarios | ADMIN | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| PATCH | /user/{id}/password | Administracao de usuarios | ADMIN | Controller unitario + service | Contrato HTTP nao validado na linha de base | P1 |
| GET | /user/{id} | Administracao de usuarios | ADMIN | Controller unitario + service | Rota herdada: contrato e autorizacao sem teste HTTP | P2 |
| DELETE | /user/{id} | Administracao de usuarios | ADMIN | Controller unitario + service | Rota herdada: contrato e autorizacao sem teste HTTP | P1 |

## Transporte e infraestrutura

- STOMP: handshake `/ws` (WebSocket/SockJS), destinos de aplicacao `/app/chat.send` e `/app/groups.send`; autenticacao no CONNECT e participacao no MessageService. Nao ha autorizacao de SUBSCRIBE no interceptor atual.
- OpenAPI: `/v3/api-docs`, Swagger UI `/swagger-ui/**`; Actuator `/actuator/health` e probes publicos. `/plans/{planCode}/limits` exige autenticacao pela configuracao atual, embora seja consulta de plano.
- Repositorios/models de shopping lists, requests e recipes nao possuem controllers CRUD proprios; nao foram inventadas rotas.

Total: 87 operacoes HTTP da aplicacao.


