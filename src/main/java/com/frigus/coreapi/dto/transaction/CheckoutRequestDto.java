package com.frigus.coreapi.dto.transaction;

import org.hibernate.validator.constraints.Length;

import com.frigus.coreapi.enums.PaymentMethod;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CheckoutRequestDto {
    @NotNull(message = "Plano deve ser informado")
    private String planCode;

    @NotNull(message = "Forma de pagamento deve ser informada")
    private PaymentMethod paymentMethod;

    @Length(min = 4, max = 4, message = "Deve ter 4 digitos")
    private String fakeCardLast4;

    @Length(min = 10, message = "Deve ter pelo menos 10 digitos")
    private String fakePixKey;

}
