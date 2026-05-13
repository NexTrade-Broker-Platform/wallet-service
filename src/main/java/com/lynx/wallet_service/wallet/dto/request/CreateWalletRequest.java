package com.lynx.wallet_service.wallet.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateWalletRequest {
    @NotNull(message = "User ID is required")
    private UUID userId;
    private String currency;
}
