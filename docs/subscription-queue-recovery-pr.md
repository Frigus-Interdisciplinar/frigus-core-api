# 🚀 Pull Request

## 📌 Contexto

Falhas técnicas na seleção de assinatura desfaziam o incremento de tentativas e devolviam transações à fila indefinidamente. A consulta também incluía assinaturas excluídas, embora o repositório já oferecesse um método com filtro de exclusão.

## 🛠️ O que foi feito?

- [x] Corrigida a consulta de assinatura no processamento e no checkout FREE para usar `DeletedAtIsNull`, mantendo erro explícito quando há mais de uma assinatura não deletada.
- [x] Adicionado lock por ID da transação e persistência do contador de falhas em transação separada, encerrando a terceira falha e ignorando mensagens de estados não PENDING.
- [x] Removida a alteração para DELINQUENT em falhas técnicas; a recusa explícita do gateway mantém sua regra anterior.
- [x] Adicionados testes de rollback/commit reais com H2 e Spring, além do patch independente para a base `origin/dev32c95`.

## 🧪 Como validar?

1. Em JDK 23 com release 17, executar `mvn -Dtest=TransactionQueueRecoveryTest,TransactionProcessorServiceTest,TransactionWorkerServiceTest,TransactionServiceTest test`. Passou no checkout preparado: 34 testes, nenhuma falha/erro/skip.
   O port no checkout WIP também passou junto dos testes de IA e recorder financeiro: 67 testes, nenhuma falha/erro/skip, em 3 min 36 s. Os 13 arquivos anteriores desses dois módulos mantiveram SHA256 idêntico.
2. Conferir `git apply --check docs/subscription-queue-recovery.patch` em checkout limpo da base `32c95fd3d9ed8bb12a349137a16a76b0be90f3f0`.
3. Em ambiente isolado, verificar que falhas técnicas resultam em contador durável 1, 2 e 3 e transação ERROR, sem modificar a assinatura associada. Testes automatizados não conectam ao banco de produção nem disparam o worker agendado.
4. Revisar o limite documentado: histórico com duas assinaturas não deletadas permanece ambíguo. Não há saneamento automático de dados ou retomada de transações já terminais.

## 🔍 Checklist de Qualidade

- [x] **Auto-review:** Alteração cirúrgica; testes e documentação revisados, sem código comentado ou logs de debug adicionados.
- [x] **Breaking Changes:** Sem nova configuração ou alteração de contrato HTTP. O limite de três tentativas passa a funcionar e erro técnico não muda a situação financeira da assinatura.
- [x] **Performance:** Consulta por ID com lock; removido o loop indefinido de tentativas. As consultas por usuário reutilizam o índice existente.

---

### 📸 Evidências Visuais

Não se aplica: correção de processamento no backend. Resultados completos e limites em `docs/subscription-queue-recovery.md`.
