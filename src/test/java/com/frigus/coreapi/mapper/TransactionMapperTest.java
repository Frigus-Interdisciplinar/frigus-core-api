package com.frigus.coreapi.mapper;

import com.frigus.coreapi.dto.transaction.CheckoutRequestDto;
import com.frigus.coreapi.enums.PaymentMethod;
import com.frigus.coreapi.enums.TransactionStatus;
import com.frigus.coreapi.model.Plan;
import com.frigus.coreapi.model.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionMapperTest {
    private final TransactionMapper mapper = new TransactionMapper();

    @Test
    void mapsCheckoutRequestsAndTransactionResponses() {
        CheckoutRequestDto checkout = CheckoutRequestDto.builder().paymentMethod(PaymentMethod.CREDIT_CARD)
                .fakeCardLast4("1234").fakePixKey("pix@example.com").build();
        var entity = mapper.toEntity(checkout);
        assertThat(entity.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
        assertThat(entity.getFakeCardLast4()).isEqualTo("1234");

        Transaction transaction = Transaction.builder().id(UUID.randomUUID()).idempotencyKey("key")
                .amount(new BigDecimal("9.99")).paymentMethod(PaymentMethod.PIX).status(TransactionStatus.APPROVED)
                .plan(Plan.builder().planCode("PLUS").build()).build();
        var response = mapper.toDto(transaction);
        assertThat(response.getPlanCode()).isEqualTo("PLUS");
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.APPROVED);
    }
}
