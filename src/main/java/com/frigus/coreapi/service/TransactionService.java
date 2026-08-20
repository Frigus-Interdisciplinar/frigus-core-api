package com.frigus.coreapi.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.frigus.coreapi.dto.transaction.CheckoutRequestDto;
import com.frigus.coreapi.dto.transaction.TransactionQueuePayload;
import com.frigus.coreapi.dto.transaction.TransactionResponseDto;
import com.frigus.coreapi.enums.PaymentMethod;
import com.frigus.coreapi.enums.TransactionStatus;
import com.frigus.coreapi.exception.BadRequestException;
import com.frigus.coreapi.exception.ForbiddenException;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.mapper.TransactionMapper;
import com.frigus.coreapi.model.Plan;
import com.frigus.coreapi.model.Transaction;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.PlanRepository;
import com.frigus.coreapi.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.util.DigestUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionQueueProducer transactionQueueProducer;
    private final PlanRepository planRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    @Transactional
    public TransactionResponseDto checkout(User currentUser, CheckoutRequestDto dto) {
        Plan plan = planRepository.findByPlanCode(dto.getPlanCode())
                .orElseThrow(() -> new BadRequestException("Plano não encontrado", "Plano com o código " + dto.getPlanCode() + " não foi encontrado"));

        if (Boolean.FALSE.equals(plan.getActive())) {
            throw new BadRequestException("Plano inativo", "Plano com o código " + dto.getPlanCode() + " não está dispoível para assinaturas");
        }

        if (dto.getPaymentMethod() == PaymentMethod.PIX && dto.getFakePixKey() == null) {
            throw new BadRequestException("Chave PIX obrigatória", "Para pagamento via PIX, informe a chave PIX.");
        }
        if (dto.getPaymentMethod() == PaymentMethod.CREDIT_CARD && dto.getFakeCardLast4() == null ) {
            throw new BadRequestException("Cartão obrigatório", "Para pagamento com cartão, informe os últimos 4 dígitos do cartão.");
        }

        String idempotencyKey = generateIdempotencyKey(currentUser.getId(), plan.getPlanCode());
        Optional<Transaction> existingTransaction = transactionRepository.findByIdempotencyKey(idempotencyKey);

        if (existingTransaction.isPresent()) {
            Transaction tx = existingTransaction.get();

            if (tx.getStatus() == TransactionStatus.PENDING || tx.getStatus() == TransactionStatus.APPROVED || tx.getStatus() == TransactionStatus.PROCESSING) {
                throw new BadRequestException("Transação pendente", "Já existe uma transação " + tx.getId() + " para o usuário e plano informados");
            }
        }

        idempotencyKey = idempotencyKey + ":retry:" + UUID.randomUUID().toString().substring(0, 8);

        Transaction transaction = Transaction.builder()
                .idempotencyKey(idempotencyKey)
                .user(currentUser)
                .plan(plan)
                .amount(plan.getPrice())
                .paymentMethod(dto.getPaymentMethod())
                .fakeCardLast4(dto.getFakeCardLast4())
                .fakePixKey(dto.getFakePixKey())
                .status(TransactionStatus.PENDING)
                .attempts(0)
                .maxAttempts(3)
                .createdAt(Instant.now())
                .queuedAt(Instant.now())
                .build();

        try {
            transaction = transactionRepository.save(transaction);
        } catch (DataIntegrityViolationException e) {
            return transactionRepository.findByIdempotencyKey(idempotencyKey)
                .map(transactionMapper::toDto)
                .orElseThrow(() -> e);
        }

        

        TransactionQueuePayload payload = TransactionQueuePayload.builder()
                .transactionId(transaction.getId())
                .idempotencyKey(transaction.getIdempotencyKey())
                .userId(transaction.getUser().getId())
                .planCode(transaction.getPlan().getPlanCode())
                .amount(transaction.getAmount())
                .paymentMethod(transaction.getPaymentMethod())
                .fakeCardLast4(transaction.getFakeCardLast4())
                .fakePixKey(transaction.getFakePixKey())
                .attempt(0)
                .build();

        transactionQueueProducer.enqueue(payload);

        return transactionMapper.toDto(transaction);
    }

    public TransactionResponseDto getTransactionById(UUID transactionId, User currentUser) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Transação não encontrada", "Transação com o id " + transactionId + " não foi encontrada"));

        if(!transaction.getUser().getId().equals(currentUser.getId()))
            throw new ForbiddenException("Transação não autorizada", "Transação com o id " + transactionId + " não pertence ao usuário");

        return transactionMapper.toDto(transaction);
    }

    private String generateIdempotencyKey(UUID userId, String planCode) {
        // vamos usar diferenca de 5 minutos na chave de idempotencia, ou seja, o usuario so vai poder fazer outra requisicao de transacao apos 5 minutos!! 5 minutos == 300 segundos
        long timeBucket = Instant.now().getEpochSecond() / 300;

        String raw = String.format("user:%s:plan:%s:bucket:%d", 
            userId, 
            planCode, 
            timeBucket);

        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }
}
 