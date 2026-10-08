# F2O-163: configuração e verificação

Consulta ao [workitem F2O-163](https://frigus-inter-2o.atlassian.net/browse/F2O-163) em 2026-10-08: a descrição menciona rate limit, CORS e proteção da API key; não há subtarefas nem comentários com critérios adicionais.

## Contratos de segurança

- `SECURITY_ALLOWED_ORIGINS` recebe uma lista de origens completas, separadas por vírgula. Sem valor, o desenvolvimento local aceita `http://localhost:3000` e `http://localhost:5173`. O deploy inclui `https://API_HOST` e exige a lista HTTPS real das origens do frontend.
- A autenticação continua aceitando `Authorization: Bearer` e cookies. Para operações mutáveis autenticadas por cookie, o cliente obtém `GET /auth/csrf` e envia o valor em `X-XSRF-TOKEN`. `POST /auth/refresh` e `POST /auth/logout` exigem CSRF quando há cookie de token; login e registro não exigem, mesmo com cookies antigos no navegador. Um Bearer explícito prevalece sobre o cookie nas demais rotas. Os cookies de token são `HttpOnly`, `Secure` e `SameSite=Lax`.
- As inscrições STOMP aceitas são `/topic/conversations.{uuid}`, `/topic/groups.{uuid}` e `/topic/groups.{uuid}.messages`. A conexão exige token válido; a inscrição exige participação ativa ou vínculo atual com o grupo. Envios aceitos: `/app/chat.send` e `/app/groups.send`.
- Os limites por janela de 60 segundos, compartilhados no Redis, são 10 chamadas por IP para cada rota de autenticação, 20 por usuário para IA e 10 por usuário para checkout. `SECURITY_RATE_LIMIT_AUTH`, `SECURITY_RATE_LIMIT_AI`, `SECURITY_RATE_LIMIT_CHECKOUT` e `SECURITY_RATE_LIMIT_WINDOW_SECONDS` ajustam esses valores. `429` inclui `Retry-After`. `SECURITY_TRUSTED_PROXY_CIDRS` indica as faixas IPv4 dos proxies que podem fornecer `X-Forwarded-For`; o filtro percorre a cadeia da direita para a esquerda e usa o último IP não confiável. Sem essa configuração, ou para IPv6, usa o endereço do socket.
- `POST /notifications/ad-clicks` exige contagem fornecida por produtor confiável. O produtor assina `userId:totalClicks:timestamp`, com timestamp Unix em segundos, usando HMAC-SHA256 e o segredo `AD_CLICK_REPORT_SECRET`. Envie o digest hexadecimal em `X-Ad-Clicks-Signature` e o timestamp em `X-Ad-Clicks-Timestamp`. A assinatura expira em cinco minutos. Uma chamada aceita avanço máximo de 10.000 cliques e total máximo de 1 bilhão; os marcos são gravados com chave única para suportar repetição e concorrência. O segredo deve ser conhecido apenas pelo produtor e pela API.
- `FRIGUS_AI_API_KEY` é a chave de saída para o serviço de IA. No perfil de deploy ela é obrigatória via `FRIGUS_AI_API_KEY_REQUIRED=true`. A chave não é incluída na resposta ou nos logs do cliente. Autenticação de entrada por API key depende de contrato específico de titular, escopo e revogação, ainda não definido na F2O-163 disponível.

## Deploy

Configure `API_HOST`, `ALB_CERTIFICATE_ARN`, `FRONTEND_ORIGINS` e `TRUSTED_PROXY_CIDRS` no ambiente de execução do script. A última variável deve conter somente as faixas reais do ALB/ingress; não use a faixa de todos os clientes. O certificado ACM deve cobrir `API_HOST`. O `.env` local deve conter, além dos segredos já existentes, `FRIGUS_AI_BASE_URL`, `FRIGUS_AI_API_KEY` e `AD_CLICK_REPORT_SECRET`. O script aplica o Ingress com HTTP 80 redirecionado para HTTPS 443 e o certificado indicado.

Execute `db/migrations/005_add_discard_quantity.sql` antes de publicar a versão. Registros antigos mantêm `quantity` nulo porque o esquema anterior não armazenava essa informação; novos descartes gravam a quantidade informada.

Antes de liberar o ambiente, valide o certificado e o redirecionamento no domínio real, o atributo `Secure` em todos os `Set-Cookie`, o preflight das origens permitidas e negadas, e a comunicação com a IA usando uma chave fictícia em homologação. Não registre tokens nem chaves na evidência. As configurações de ALB, ACM, DNS e do produtor de cliques exigem recursos externos e não podem ser comprovadas por testes locais.

A política de exposição pública do Swagger e de edição de receitas/ingredientes não consta da F2O-163 consultada. Essas permissões precisam de uma decisão de produto antes de mudança.
