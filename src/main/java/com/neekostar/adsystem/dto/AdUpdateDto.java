package com.neekostar.adsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "AdUpdateDto", description = "DTO for updating an advertisement")
public class AdUpdateDto {
    @Size(min = 3, max = 255, message = "{ad.title.size}")
    @Schema(description = "Title of the advertisement", example = "Car for sale", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Size(max = 1000, message = "{ad.description.size}")
    @Schema(description = "Description of the advertisement", example = "Good condition, low mileage", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String description;

    @DecimalMin(value = "0.0", inclusive = false, message = "{ad.price.positive}")
    @Schema(description = "Price of the advertisement", example = "10000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal price;

    @Pattern(regexp = "^[A-ZА-ЯЁ][a-zа-яё]+$", message = "{ad.city.pattern}")
    @Schema(description = "City of the advertisement", example = "Moscow", requiredMode = Schema.RequiredMode.REQUIRED)
    private String city;

    @Schema(description = "Category of the advertisement", example = "VEHICLES", requiredMode = Schema.RequiredMode.REQUIRED)
    private String category;
}
