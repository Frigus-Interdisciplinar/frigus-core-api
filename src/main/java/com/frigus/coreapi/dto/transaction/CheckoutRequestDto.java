package com.frigus.coreapi.dto.transaction;

import org.hibernate.validator.constraints.Length;

import com.frigus.coreapi.enums.PaymentMethod;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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

    @Pattern(regexp = "^\\d{4}$", message = "Os últimos 4 dígitos do cartão devem conter exatamente 4 números")
    private String fakeCardLast4;

    @Size(min = 10, max = 150, message = "A chave PIX deve ter entre 10 e 150 caracteres")
    private String fakePixKey;
}
