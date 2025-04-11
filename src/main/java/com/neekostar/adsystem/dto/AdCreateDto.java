package com.neekostar.adsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "AdCreateDto", description = "DTO for creating a new advertisement")
public class AdCreateDto {
    @NotBlank(message = "{ad.title.notblank}")
    @Size(min = 3, max = 255, message = "{ad.title.size}")
    @Schema(description = "Title of the advertisement", example = "Car for sale", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Size(max = 1000, message = "{ad.description.size}")
    @Schema(description = "Description of the advertisement", example = "Good condition, low mileage", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String description;

    @NotNull(message = "{ad.price.notnull}")
    @DecimalMin(value = "0.0", inclusive = false, message = "{ad.price.positive}")
    @Schema(description = "Price of the advertisement", example = "10000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal price;

    @NotBlank(message = "{ad.city.notblank}")
    @Pattern(regexp = "^[A-ZА-ЯЁ][a-zа-яё]+$", message = "{ad.city.pattern}")
    @Schema(description = "City of the advertisement", example = "Moscow", requiredMode = Schema.RequiredMode.REQUIRED)
    private String city;

    @NotBlank(message = "{ad.category.notblank}")
    @Schema(description = "Category of the advertisement", example = "VEHICLES", requiredMode = Schema.RequiredMode.REQUIRED)
    private String category;
}
