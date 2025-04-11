package com.neekostar.adsystem.controller;

import java.util.List;
import java.util.UUID;
import com.neekostar.adsystem.dto.ErrorResponse;
import com.neekostar.adsystem.dto.ReviewCreateDto;
import com.neekostar.adsystem.dto.ReviewResponseDto;
import com.neekostar.adsystem.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@Tag(
        name = "Rating Management",
        description = "This controller handles all operations related to reviews for completed sales. <br><br>" +
                "The available operations include: <ul>" +
                "<li><b>Create Rating</b> – Allows a buyer to rate a completed sale. The operation verifies that " +
                "the authenticated user is the buyer in the sale history, ensures that a review has not already been submitted, " +
                "and then creates a new review. It also recalculates and updates the seller's overall review based on all " +
                "received reviews.</li>" +
                "<li><b>Get Ratings by Sale</b> – Retrieves all reviews associated with a specific sale. The result is " +
                "returned as a list of review details.</li>" +
                "</ul>" +
                "Possible exceptions include: <br><br>" +
                "<b>ResourceNotFoundException</b> – when the sale history or user is not found; <br>" +
                "<b>AccessDeniedException</b> – when the authenticated user is not the buyer or has already rated the sale; <br>" +
                "<b>IllegalArgumentException</b> – if the review value is invalid."
)
public class ReviewController {

    private final ReviewService reviewService;

    @Autowired
    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/sales/{saleId}")
    @Operation(
            summary = "Create Rating",
            description = "Creates a new review for the specified sale. The authenticated user must be the buyer " +
                    "associated with the sale. Additionally, the user must not have already rated this sale. " +
                    "Upon success, the seller's overall review is recalculated and updated. " +
                    "Possible errors include: <ul>" +
                    "<li><b>ResourceNotFoundException</b> – if the sale or user is not found.</li>" +
                    "<li><b>AccessDeniedException</b> – if the authenticated user is not the buyer or has already rated the sale.</li>" +
                    "<li><b>IllegalArgumentException</b> – if the review value is out of the accepted range.</li>" +
                    "</ul>",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Rating data including review value and optional review text",
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ReviewCreateDto.class)
                    )
            ),
            parameters = {
                    @io.swagger.v3.oas.annotations.Parameter(
                            name = "saleId",
                            description = "Unique identifier (UUID) of the completed sale to rate",
                            required = true
                    )
            },
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "201",
                            description = "Rating created successfully",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ReviewResponseDto.class)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Invalid review data (e.g., review value out of range)",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "BadRequestExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 400,
                                                              "error": "Bad Request",
                                                              "message": "Rating value must be between 1 and 5",
                                                              "path": "/api/reviews/sales/11111111-2222-3333-4444-555555555555",
                                                              "method": "POST"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "403",
                            description = "Access denied. Either the authenticated user is not the buyer or has already rated this sale",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "ForbiddenExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 403,
                                                              "error": "Forbidden",
                                                              "message": "You have already rated this sale",
                                                              "path": "/api/reviews/sales/11111111-2222-3333-4444-555555555555",
                                                              "method": "POST"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "Sale or user not found",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "NotFoundExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 404,
                                                              "error": "Not Found",
                                                              "message": "Sale history not found",
                                                              "path": "/api/reviews/sales/11111111-2222-3333-4444-555555555555",
                                                              "method": "POST"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    public ResponseEntity<?> createRating(@PathVariable UUID saleId,
                                          @Validated @RequestBody ReviewCreateDto reviewCreateDto) {
        ReviewResponseDto ratingResponse = reviewService.createRating(saleId, reviewCreateDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ratingResponse);
    }

    @GetMapping("/sales/{saleId}")
    @Operation(
            summary = "Get Ratings by Sale",
            description = "Retrieves all reviews associated with the specified sale. " +
                    "Returns a list of reviews given for the sale. " +
                    "Possible error: 404 if no reviews or sale are found.",
            parameters = {
                    @io.swagger.v3.oas.annotations.Parameter(
                            name = "saleId",
                            description = "Unique identifier (UUID) of the sale",
                            required = true
                    )
            },
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Ratings retrieved successfully",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ReviewResponseDto.class)
                                    )
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "Ratings not found for the specified sale",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "NotFoundExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 404,
                                                              "error": "Not Found",
                                                              "message": "No reviews found for this sale",
                                                              "path": "/api/reviews/sales/11111111-2222-3333-4444-555555555555",
                                                              "method": "GET"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    public ResponseEntity<?> getRatingsBySale(@PathVariable UUID saleId) {
        List<ReviewResponseDto> ratingResponse = reviewService.getRatingsBySaleId(saleId);
        return ResponseEntity.status(HttpStatus.OK).body(ratingResponse);
    }
}
