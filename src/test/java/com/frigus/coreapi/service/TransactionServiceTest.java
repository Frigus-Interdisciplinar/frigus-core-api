package com.frigus.coreapi.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionQueueProducer transactionQueueProducer;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionService transactionService;

    private User currentUser;
    private Plan plan;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id(UUID.randomUUID())
                .name("Gabriel Test")
                .email("gabriel@test.com")
                .build();

        plan = Plan.builder()
                .id(1)
                .planCode("PREMIUM")
                .name("Plano Premium")
                .price(new BigDecimal("49.90"))
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Deve realizar checkout com sucesso para nova transação")
    void shouldCheckoutSuccessfullyForNewTransaction() {
        CheckoutRequestDto dto = CheckoutRequestDto.builder()
                .planCode("PREMIUM")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .fakeCardLast4("1234")
                .build();

        Transaction savedTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .idempotencyKey("idemp-test")
                .user(currentUser)
                .plan(plan)
                .amount(plan.getPrice())
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .fakeCardLast4("1234")
                .status(TransactionStatus.PENDING)
                .build();

        TransactionResponseDto responseDto = TransactionResponseDto.builder()
                .id(savedTransaction.getId())
                .status(TransactionStatus.PENDING)
                .amount(plan.getPrice())
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .idempotencyKey("idemp-test")
                .build();

        when(planRepository.findByPlanCode("PREMIUM")).thenReturn(Optional.of(plan));
        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);
        when(transactionMapper.toDto(savedTransaction)).thenReturn(responseDto);

        TransactionResponseDto result = transactionService.checkout(currentUser, dto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(savedTransaction.getId());
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.PENDING);

        verify(transactionRepository).save(any(Transaction.class));
        verify(transactionQueueProducer).enqueue(any(TransactionQueuePayload.class));
        verify(transactionMapper).toDto(savedTransaction);
    }

    @Test
    @DisplayName("Deve retornar transação existente quando chave de idempotência já existir (sem duplicar)")
    void shouldReturnExistingTransactionWhenIdempotent() {
        CheckoutRequestDto dto = CheckoutRequestDto.builder()
                .planCode("PREMIUM")
                .paymentMethod(PaymentMethod.PIX)
                .fakePixKey("teste@pix.com")
                .build();

        Transaction existingTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .idempotencyKey("existing-key")
                .user(currentUser)
                .plan(plan)
                .amount(plan.getPrice())
                .status(TransactionStatus.PENDING)
                .build();

        TransactionResponseDto responseDto = TransactionResponseDto.builder()
                .id(existingTransaction.getId())
                .status(TransactionStatus.PENDING)
                .build();

        when(planRepository.findByPlanCode("PREMIUM")).thenReturn(Optional.of(plan));
        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.of(existingTransaction));
        when(transactionMapper.toDto(existingTransaction)).thenReturn(responseDto);

        TransactionResponseDto result = transactionService.checkout(currentUser, dto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(existingTransaction.getId());

        // Não deve salvar nova nem enfileirar
        verify(transactionRepository, never()).save(any());
        verify(transactionQueueProducer, never()).enqueue(any());
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando plano não for encontrado")
    void shouldThrowExceptionWhenPlanNotFound() {
        CheckoutRequestDto dto = CheckoutRequestDto.builder()
                .planCode("INVALID_PLAN")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .fakeCardLast4("1234")
                .build();

        when(planRepository.findByPlanCode("INVALID_PLAN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.checkout(currentUser, dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Plano não encontrado");
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando PIX não tiver fakePixKey")
    void shouldThrowExceptionWhenPixMissingKey() {
        CheckoutRequestDto dto = CheckoutRequestDto.builder()
                .planCode("PREMIUM")
                .paymentMethod(PaymentMethod.PIX)
                .fakePixKey(null)
                .build();

        when(planRepository.findByPlanCode("PREMIUM")).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> transactionService.checkout(currentUser, dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Chave PIX obrigatória");
    }

    @Test
    @DisplayName("Deve lançar BadRequestException quando Cartão não tiver fakeCardLast4")
    void shouldThrowExceptionWhenCardMissingLast4() {
        CheckoutRequestDto dto = CheckoutRequestDto.builder()
                .planCode("PREMIUM")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .fakeCardLast4(null)
                .build();

        when(planRepository.findByPlanCode("PREMIUM")).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> transactionService.checkout(currentUser, dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cartão obrigatório");
    }

    @Test
    @DisplayName("Deve buscar transação por ID com sucesso para o dono da transação")
    void shouldGetTransactionByIdSuccessfully() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .user(currentUser)
                .plan(plan)
                .status(TransactionStatus.APPROVED)
                .build();

        TransactionResponseDto responseDto = TransactionResponseDto.builder()
                .id(transactionId)
                .status(TransactionStatus.APPROVED)
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(transactionMapper.toDto(transaction)).thenReturn(responseDto);

        TransactionResponseDto result = transactionService.getTransactionById(transactionId, currentUser);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(transactionId);
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao buscar transação inexistente")
    void shouldThrowNotFoundWhenTransactionDoesNotExist() {
        UUID transactionId = UUID.randomUUID();
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getTransactionById(transactionId, currentUser))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Transação não encontrada");
    }

    @Test
    @DisplayName("Deve lançar ForbiddenException ao tentar consultar transação de outro usuário")
    void shouldThrowForbiddenWhenUserIsNotOwner() {
        UUID transactionId = UUID.randomUUID();
        User otherUser = User.builder().id(UUID.randomUUID()).build();

        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .user(otherUser)
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> transactionService.getTransactionById(transactionId, currentUser))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Transação não autorizada");
    }
}
