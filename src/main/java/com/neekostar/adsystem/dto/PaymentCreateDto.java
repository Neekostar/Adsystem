package com.neekostar.adsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "PaymentCreateDto", description = "DTO for creating a payment")
public class PaymentCreateDto {
    @NotBlank(message = "{ad.id.notblank}")
    @Schema(description = "Advertisement ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private String adId;

    @NotNull(message = "{payment.amount.notnull}")
    @DecimalMin(value = "0.0", inclusive = false, message = "{payment.amount.positive}")
    @Schema(description = "Amount of the payment", example = "100.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;
}
