# 🚀 Pull Request

## 📌 Contexto
> Preservar o contexto real de usuário/estoque no chat mobile, registrar o custo confirmado de novos lotes para o MoneySaving e corrigir a repetição indefinida de transações após rollback. Cada responsabilidade está separada em commit; nenhuma operação de pagamento ou saneamento de dados de produção foi executada.

## 🛠️ O que foi feito?
- [x] Feature X adicionada / Bug Y corrigido
- [x] Refatoração do componente Z
- [x] Atualização de dependências ou variáveis de ambiente

- Chat: ponte interna autenticada ao Python envia identidade, estoque, conversa e preferências; somente IDs explícitos de receitas do turno são persistidos. Diagnósticos internos preservam status/classe da causa e não registram prompts, corpos de erro ou credenciais.
- Estoque/MoneySaving: criação de lote com preço confirmado registra aquisição, linha, supply e ledger na mesma transação, sem somar o saldo novamente. Falhas revertem todas as gravações; criação sem preço não inventa custo.
- Fila: lookup de assinatura desconsidera soft-deleted; lock por transação e guardas de estado protegem mensagens duplicadas. O contador de falhas é persistido em transação separada e encerra a terceira falha em ERROR. Erro técnico não muda a assinatura para DELINQUENT; recusa explícita do gateway conserva o comportamento anterior.
- Duas assinaturas não deletadas continuam exigindo revisão; não há escolha arbitrária, exclusão de histórico, reprocessamento automático ou nova política de seleção.
- Artefatos seletivos sobre `origin/dev` em `ai-chat-origin-dev.patch` e `subscription-queue-recovery.patch`, separados por responsabilidade. O primeiro preserva integralmente a extração NFC-e mais recente e separa as credenciais pública/interna.

## 🧪 Como validar?
1. No checkout de trabalho, validação conjunta após o port da fila: **67 testes aprovados, zero falhas, erros ou ignorados**, em 3m36s (34 fila, 24 IA e 9 financeiros). Comando: `mvn -Dtest=TransactionQueueRecoveryTest,TransactionProcessorServiceTest,TransactionWorkerServiceTest,TransactionServiceTest,FrigusAiClientTest,AiRecipeChatServiceTest,StockProductServiceTest,StockPurchaseRecorderTest test`. Essa execução compila o código efetivamente editado; não equivale à suíte inteira do Core.
2. No checkout isolado `origin/dev` (`32c95fd`), IA e notas: `mvn -Dtest=FrigusAiClientTest,AiRecipeChatServiceTest,ReceiptServiceTest test` — 38 testes aprovados, incluindo NFC-e e separação de credenciais.
3. No checkout isolado `origin/dev`, fila: `mvn -Dtest=TransactionQueueRecoveryTest,TransactionProcessorServiceTest,TransactionWorkerServiceTest,TransactionServiceTest test` — 34 testes aprovados, incluindo rollback real H2/Spring, falhas 1→2→3, parada em ERROR e assinatura preservada.
4. Aplicação/reversão dos patches verificadas em checkouts isolados; 13 hashes do WIP anterior confirmam que o port da fila preservou IA e integração financeira. Testes usam JDK 23 com release 17, sem banco de produção ou pagamentos reais.
5. Pendente após integrar na revisão de destino e configurar os serviços: validar chat/receitas no aparelho, criação de lote e custo no MoneySaving, e observar a fila. Não repetir transações financeiras existentes sem análise explícita.

## 🔍 Checklist de Qualidade
- [x] **Auto-review:** Deletei códigos comentados, logs de debug e limpei imports não usados.
- [x] **Breaking Changes:** Essa alteração exige novas variáveis de ambiente (`.env`) ou quebra compatibilidade?
- [x] **Performance:** Garanti que não há queries, renderizações redundantes ou loops desnecessários.

O checkout de trabalho parte de `f824262`, anterior à extração NFC-e de `origin/dev`; não substituir o cliente mais recente por esse arquivo antigo. Para integrar no `dev` atual, usar a adaptação seletiva documentada em `ai-chat-origin-dev.md` e conferir novamente os patches no destino. O teste dos patches não equivale a deploy.

Configuração do artefato atual: `FRIGUS_AI_BASE_URL`, chave pública existente `FRIGUS_AI_API_KEY` para NFC-e e chave interna dedicada `FRIGUS_AI_CORE_SERVICE_KEY`, igual à `API_KEYS__CORE_SERVICE_KEY` do Python. O checkout antigo usa `FRIGUS_AI_API_KEY` como chave da ponte, conforme seu `.env.example`; os dois formatos não devem ser misturados. Não há chaves reais versionadas. Não foram alterados modelos pagos ou variáveis do Render.

Os logs adicionados são diagnósticos operacionais sanitizados, sem payload sensível. O lock é por transação; não há replay automático de chat ou geração automática de nova transação.

---
### 📸 Evidências Visuais
<details>
<summary>Clique para expandir</summary>

Sem interface visual neste repositório. Evidências e limites estão em `ai-chat-provider-diagnostics.md`, `ai-chat-origin-dev.md`, `stock-purchase-recording.md` e `subscription-queue-recovery.md`. Validação em produção permanece pendente.

</details>
