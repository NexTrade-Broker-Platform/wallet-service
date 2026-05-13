package com.lynx.wallet_service.wallet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DepositRequest {
    @NotNull(message = "Amount is required")
    @Positive(message = "Deposit amount must be strictly greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "[A-Z]{3}", message = "Currency must be a 3-letter ISO 4217 code")
    private String currency;
}
