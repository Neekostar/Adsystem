package com.neekostar.adsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Schema(name = "PaymentResponseDto", description = "Payment response data transfer object")
public class PaymentResponseDto implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "Payment amount", example = "100.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @Schema(description = "Payment date", example = "2021-07-01T12:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime paymentDate;

    @Schema(description = "Advertisement ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID adId;
}
