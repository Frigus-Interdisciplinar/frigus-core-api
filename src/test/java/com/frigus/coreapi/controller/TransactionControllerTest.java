package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.transaction.CheckoutRequestDto;
import com.frigus.coreapi.dto.transaction.TransactionResponseDto;
import com.frigus.coreapi.enums.PaymentMethod;
import com.frigus.coreapi.enums.TransactionStatus;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionController transactionController;

    @Test
    @DisplayName("Deve delegar checkout para o TransactionService com sucesso")
    void shouldDelegateCheckoutToService() {
        User user = User.builder().id(UUID.randomUUID()).build();
        CheckoutRequestDto requestDto = CheckoutRequestDto.builder()
                .planCode("PRO")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .fakeCardLast4("1234")
                .build();

        TransactionResponseDto expectedResponse = TransactionResponseDto.builder()
                .id(UUID.randomUUID())
                .status(TransactionStatus.PENDING)
                .amount(new BigDecimal("99.90"))
                .build();

        when(transactionService.checkout(user, requestDto, "custom-key")).thenReturn(expectedResponse);

        TransactionResponseDto response = transactionController.checkout(requestDto, "custom-key", user);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(expectedResponse.getId());
        verify(transactionService).checkout(user, requestDto, "custom-key");
    }

    @Test
    @DisplayName("Deve delegar listagem de transações para o TransactionService")
    void shouldDelegateListTransactionsToService() {
        User user = User.builder().id(UUID.randomUUID()).build();
        Pageable pageable = PageRequest.of(0, 10);
        Page<TransactionResponseDto> expectedPage = new PageImpl<>(List.of(
                TransactionResponseDto.builder().id(UUID.randomUUID()).build()
        ));

        when(transactionService.listTransactions(user, pageable)).thenReturn(expectedPage);

        Page<TransactionResponseDto> response = transactionController.listTransactions(pageable, user);

        assertThat(response).isNotNull();
        assertThat(response.getTotalElements()).isEqualTo(1);
        verify(transactionService).listTransactions(user, pageable);
    }

    @Test
    @DisplayName("Deve delegar busca de transação por ID para o TransactionService")
    void shouldDelegateGetTransactionByIdToService() {
        User user = User.builder().id(UUID.randomUUID()).build();
        UUID transactionId = UUID.randomUUID();

        TransactionResponseDto expectedResponse = TransactionResponseDto.builder()
                .id(transactionId)
                .status(TransactionStatus.APPROVED)
                .build();

        when(transactionService.getTransactionById(transactionId, user)).thenReturn(expectedResponse);

        TransactionResponseDto response = transactionController.getTransactionById(transactionId, user);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(transactionId);
        verify(transactionService).getTransactionById(transactionId, user);
    }

    @Test
    @DisplayName("Deve delegar cancelamento de transação para o TransactionService")
    void shouldDelegateCancelTransactionToService() {
        User user = User.builder().id(UUID.randomUUID()).build();
        UUID transactionId = UUID.randomUUID();

        TransactionResponseDto expectedResponse = TransactionResponseDto.builder()
                .id(transactionId)
                .status(TransactionStatus.CANCELED)
                .build();

        when(transactionService.cancelTransaction(transactionId, user)).thenReturn(expectedResponse);

        TransactionResponseDto response = transactionController.cancelTransaction(transactionId, user);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(transactionId);
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.CANCELED);
        verify(transactionService).cancelTransaction(transactionId, user);
    }
}
