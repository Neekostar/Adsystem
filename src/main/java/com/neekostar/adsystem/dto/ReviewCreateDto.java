package com.neekostar.adsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(name = "ReviewCreateDto", description = "DTO for creating a rating")
public class ReviewCreateDto {
    @NotNull(message = "{review.value.notnull}")
    @Min(value = 1, message = "{rating.value.min}")
    @Max(value = 5, message = "{rating.value.max}")
    @Schema(description = "Rating value", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer ratingValue;

    @Size(max = 1000, message = "{review.size}")
    @Schema(description = "Review text", example = "Nice!", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String reviewText;
}
