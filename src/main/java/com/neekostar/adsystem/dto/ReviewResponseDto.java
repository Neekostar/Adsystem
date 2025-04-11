package com.neekostar.adsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
@Schema(name = "ReviewResponseDto", description = "DTO for rating response")
public class ReviewResponseDto implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "Username of the review author", example = "john_doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(description = "Username of the seller", example = "jane_doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sellerUsername;

    @Schema(description = "Sale ID", example = "123e4567-e89b-12d3-a456-426614174000", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID saleId;

    @Schema(description = "Rating value", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer ratingValue;

    @Schema(description = "Review text", example = "Nice!", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String reviewText;

    @Schema(description = "Date of the review creation", example = "2021-07-01T12:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private String createdAt;
}
