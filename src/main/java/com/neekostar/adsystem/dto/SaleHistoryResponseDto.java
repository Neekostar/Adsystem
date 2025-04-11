package com.neekostar.adsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Schema(name = "SaleHistoryResponseDto", description = "DTO for sale history response")
public class SaleHistoryResponseDto implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "Sale ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID saleId;

    @Schema(description = "Username of the seller", example = "john_doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sellerUsername;

    @Schema(description = "Ad data", requiredMode = Schema.RequiredMode.REQUIRED)
    private AdResponseDto ad;

    @Schema(description = "Title of the advertisement", example = "Car", requiredMode = Schema.RequiredMode.REQUIRED)
    private String adTitle;

    @Schema(description = "Username of the buyer", example = "jane_doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String buyerUsername;

    @Schema(description = "Date of the sale", example = "2021-07-01T12:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime saleDate;
}
