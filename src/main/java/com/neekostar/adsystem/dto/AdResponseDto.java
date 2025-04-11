package com.neekostar.adsystem.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import com.neekostar.adsystem.model.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "AdResponseDto", description = "DTO for displaying an advertisement")
public class AdResponseDto implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "Unique identifier of the advertisement", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Title of the advertisement", example = "Car for sale")
    private String title;

    @Schema(description = "Description of the advertisement", example = "Good condition, low mileage")
    private String description;

    @Schema(description = "Price of the advertisement", example = "10000.00")
    private BigDecimal price;

    @Schema(description = "City of the advertisement", example = "Moscow")
    private String city;

    @Schema(description = "Category of the advertisement", example = "VEHICLES")
    private Category category;

    @Schema(description = "Username of the advertisement owner", example = "john_doe")
    private String username;

    @Schema(description = "Indicates if the advertisement is promoted", example = "true")
    private Boolean isPromoted;

    @Schema(description = "End date of the promotion", example = "2022-12-31T23:59:59")
    private LocalDateTime promotionEndDate;

    @Schema(description = "List of comments associated with the advertisement")
    private List<CommentResponseDto> comments;

    @Schema(description = "Status of the advertisement", example = "ACTIVE")
    private String status;

    @Schema(description = "URL of the advertisement image", example = "http://localhost:8080/api/v1/ads/123e4567-e89b-12d3-a456-426614174000/image")
    private String imageUrl;

    @Schema(description = "Rating of the advertisement owner", example = "4.5")
    private Float userRating;
}
