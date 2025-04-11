package com.neekostar.adsystem.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import com.neekostar.adsystem.dto.AdCreateDto;
import com.neekostar.adsystem.dto.AdResponseDto;
import com.neekostar.adsystem.dto.AdUpdateDto;
import com.neekostar.adsystem.dto.CommentCreateDto;
import com.neekostar.adsystem.dto.ErrorResponse;
import com.neekostar.adsystem.dto.ImageUploadDto;
import com.neekostar.adsystem.model.Category;
import com.neekostar.adsystem.service.AdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ads")
@Tag(
        name = "Ad Management",
        description = "This controller handles all operations related to advertisement management, including creating, updating, deleting ads, " +
                "filtering, promoting, commenting, and managing ad images. <br><br>" +
                "The available operations include: <ul>" +
                "<li><b>Create Ad</b> – Register a new advertisement. Ensures that the required fields are provided and that the ad is associated with the authenticated user.</li>" +
                "<li><b>Update Ad</b> – Modify details of an existing advertisement. Only the ad owner can update the ad.</li>" +
                "<li><b>Delete Ad</b> – Remove an advertisement along with its related comments. Only allowed for the ad owner.</li>" +
                "<li><b>Filter Ads</b> – Retrieve a list of ads based on various criteria such as city, category, price range, and keywords. " +
                "Results can be sorted by allowed fields (price, createdAt, updatedAt).</li>" +
                "<li><b>Promote Ad</b> – Mark an ad as promoted for a specified number of days, increasing its visibility. " +
                "Only the ad owner can promote an ad, and the number of days must be a positive integer.</li>" +
                "<li><b>Comment on Ad</b> – Add a comment to an advertisement. The comment is associated with the currently authenticated user.</li>" +
                "<li><b>Image Management</b> – Upload or remove images for an advertisement. If an image already exists, it is removed before the new one is uploaded. " +
                "Only the ad owner is permitted to perform these operations.</li>" +
                "</ul>" +
                "Possible exceptions include: <br><br>" +
                "<b>ResourceNotFoundException</b> – when an ad, user, or related resource is not found; <br>" +
                "<b>AccessDeniedException</b> – when the current user is not authorized to perform the requested operation; <br>" +
                "<b>InvalidArgumentException</b> – for invalid input data (e.g., negative promotion duration); and <br>" +
                "<b>IOException</b> – in case of file operation failures."
)
public class AdController {
    private final AdService adService;

    @Autowired
    public AdController(AdService adService) {
        this.adService = adService;
    }

    @PostMapping
    @Operation(
            summary = "Create a new advertisement",
            description = "Creates a new advertisement with the provided data. " +
                    "The advertisement is associated with the currently authenticated user. " +
                    "On success, the created advertisement is returned.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Advertisement data for creating a new ad",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AdCreateDto.class))
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Advertisement created successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AdResponseDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid data provided",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "BadRequestExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 400,
                                                              "error": "Bad Request",
                                                              "message": "Validation failed",
                                                              "errors": ["title must not be blank","price must be positive"],
                                                              "path": "/api/ads",
                                                              "method": "POST"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "User not found or related resource not found",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "NotFoundExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 404,
                                                              "error": "Not Found",
                                                              "message": "User not found",
                                                              "path": "/api/ads",
                                                              "method": "POST"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Access denied. The user is not authorized to perform this action",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "ForbiddenExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 403,
                                                              "error": "Forbidden",
                                                              "message": "Access denied",
                                                              "path": "/api/ads",
                                                              "method": "POST"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    public ResponseEntity<?> createAd(@Validated @RequestBody AdCreateDto adCreateDto) {
        AdResponseDto adResponseDto = adService.createAd(adCreateDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(adResponseDto);
    }

    @GetMapping("/{adId}")
    @Operation(
            summary = "Retrieve an advertisement by ID",
            description = "Fetches a specific advertisement using its unique identifier. Returns detailed advertisement data if found.",
            parameters = {
                    @Parameter(name = "adId", description = "Unique identifier (UUID) of the advertisement", required = true)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Advertisement retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AdResponseDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Advertisement not found",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "NotFoundExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 404,
                                                              "error": "Not Found",
                                                              "message": "Advertisement not found",
                                                              "path": "/api/ads/11111111-2222-3333-4444-555555555555",
                                                              "method": "GET"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    public ResponseEntity<?> getAd(@PathVariable UUID adId) {
        AdResponseDto adResponseDto = adService.getAd(adId);
        return ResponseEntity.status(HttpStatus.OK).body(adResponseDto);
    }

    @PutMapping("/{adId}")
    @Operation(
            summary = "Update an existing advertisement",
            description = "Updates the advertisement details (title, description, price). " +
                    "The authenticated user must be the owner of the advertisement.",
            parameters = {
                    @Parameter(name = "adId", description = "Unique identifier (UUID) of the advertisement to update", required = true)
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Advertisement data to update. Only non-null fields will be updated.",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AdUpdateDto.class))
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Advertisement updated successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AdResponseDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid data provided",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "BadRequestExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 400,
                                                              "error": "Bad Request",
                                                              "message": "Validation failed",
                                                              "errors": ["title must not be blank","price must be positive"],
                                                              "path": "/api/ads/11111111-2222-3333-4444-555555555555",
                                                              "method": "PUT"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Advertisement not found",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "NotFoundExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 404,
                                                              "error": "Not Found",
                                                              "message": "Advertisement not found",
                                                              "path": "/api/ads/11111111-2222-3333-4444-555555555555",
                                                              "method": "PUT"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Access denied. The authenticated user is not the owner",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "ForbiddenExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 403,
                                                              "error": "Forbidden",
                                                              "message": "You can only update your own ads",
                                                              "path": "/api/ads/11111111-2222-3333-4444-555555555555",
                                                              "method": "PUT"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    public ResponseEntity<?> updateAd(@PathVariable UUID adId,
                                      @Validated @RequestBody AdUpdateDto adUpdateDto) {
        AdResponseDto adResponseDto = adService.updateAd(adId, adUpdateDto);
        return ResponseEntity.status(HttpStatus.OK).body(adResponseDto);
    }

    @DeleteMapping("/{adId}")
    @Operation(
            summary = "Delete an advertisement",
            description = "Deletes the specified advertisement. " +
                    "Allowed only if the authenticated user is the owner.",
            parameters = {
                    @Parameter(name = "adId", description = "Unique identifier (UUID) of the advertisement to delete", required = true)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "Advertisement deleted successfully"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Advertisement not found",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "NotFoundExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 404,
                                                              "error": "Not Found",
                                                              "message": "Advertisement not found",
                                                              "path": "/api/ads/11111111-2222-3333-4444-555555555555",
                                                              "method": "DELETE"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Access denied. The authenticated user is not the owner",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "ForbiddenExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 403,
                                                              "error": "Forbidden",
                                                              "message": "You can only delete your own ads",
                                                              "path": "/api/ads/11111111-2222-3333-4444-555555555555",
                                                              "method": "DELETE"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    public ResponseEntity<?> deleteAd(@PathVariable UUID adId) {
        adService.deleteAd(adId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping
    @Operation(
            summary = "Retrieve advertisements with filters and sorting",
            description = "Returns a list of advertisements filtered by city, category, price range, and keyword. " +
                    "Results can be sorted by allowed fields in ascending or descending order.",
            parameters = {
                    @Parameter(name = "city", description = "Filter advertisements by city"),
                    @Parameter(name = "category", description = "Filter by advertisement category"),
                    @Parameter(name = "minPrice", description = "Minimum price"),
                    @Parameter(name = "maxPrice", description = "Maximum price"),
                    @Parameter(name = "keyword", description = "Keyword to search in advertisement title/description"),
                    @Parameter(name = "sortBy", description = "Field to sort by"),
                    @Parameter(name = "sortDir", description = "Sort direction (asc or desc)")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Advertisements retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = AdResponseDto.class)))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid filter or sort parameters provided",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "BadRequestExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 400,
                                                              "error": "Bad Request",
                                                              "message": "Invalid parameter: minPrice must be positive",
                                                              "path": "/api/ads",
                                                              "method": "GET"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    public ResponseEntity<?> getAllAds(@RequestParam(required = false) String city,
                                       @RequestParam(required = false) Category category,
                                       @RequestParam(required = false) BigDecimal minPrice,
                                       @RequestParam(required = false) BigDecimal maxPrice,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(defaultValue = "createdAt") String sortBy,
                                       @RequestParam(defaultValue = "desc") String sortDir,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AdResponseDto> adsPage = adService.filterAds(city, category, minPrice, maxPrice, keyword, sortBy, sortDir, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(adsPage);
    }

    @GetMapping("/user/{username}")
    @Operation(
            summary = "Retrieve advertisements by user with pagination",
            description = "Retrieves active advertisements created by the specified user. " +
                    "Supports pagination using query parameters 'page' and 'size'. " +
                    "If no advertisements are found, an empty page is returned.",
            parameters = {
                    @Parameter(name = "username", description = "Username of the advertisement creator", required = true),
                    @Parameter(name = "page", description = "Page number (0-indexed)", required = false, example = "0"),
                    @Parameter(name = "size", description = "Number of items per page", required = false, example = "10")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Advertisements retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = AdResponseDto.class)))),
                    @ApiResponse(
                            responseCode = "404",
                            description = "User not found",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "NotFoundExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 404,
                                                              "error": "Not Found",
                                                              "message": "User not found",
                                                              "path": "/api/ads/user/johndoe",
                                                              "method": "GET"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    public ResponseEntity<?> getAdsByUser(@PathVariable String username,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AdResponseDto> adsPage = adService.getAdsByUser(username, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(adsPage);
    }

    @PostMapping("/{adId}/promote")
    @Operation(
            summary = "Promote an advertisement",
            description = "Promotes the specified advertisement for a given number of days. " +
                    "The authenticated user must be the owner. Days must be a positive integer.",
            parameters = {
                    @Parameter(name = "adId", description = "Unique identifier of the advertisement", required = true),
                    @Parameter(name = "days", description = "Number of days to promote", required = true)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Advertisement promoted successfully"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid promotion duration provided (days <= 0)",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "BadRequestExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 400,
                                                              "error": "Bad Request",
                                                              "message": "Days must be a positive integer",
                                                              "path": "/api/ads/11111111-2222-3333-4444-555555555555/promote",
                                                              "method": "POST"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Advertisement not found",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "NotFoundExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 404,
                                                              "error": "Not Found",
                                                              "message": "Advertisement not found",
                                                              "path": "/api/ads/11111111-2222-3333-4444-555555555555/promote",
                                                              "method": "POST"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Access denied. The authenticated user is not the owner",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "ForbiddenExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 403,
                                                              "error": "Forbidden",
                                                              "message": "You can only promote your own ads",
                                                              "path": "/api/ads/11111111-2222-3333-4444-555555555555/promote",
                                                              "method": "POST"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    public ResponseEntity<?> promoteAd(@PathVariable UUID adId,
                                       @RequestParam int days) {
        adService.promoteAd(adId, days);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/promoted")
    @Operation(
            summary = "Retrieve all promoted advertisements",
            description = "Returns a list of all advertisements currently promoted (promotion still active).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Promoted advertisements retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = AdResponseDto.class))))
            }
    )
    public ResponseEntity<?> getPromotedAds() {
        List<AdResponseDto> ads = adService.getPromotedAds();
        return ResponseEntity.status(HttpStatus.OK).body(ads);
    }

    @GetMapping("/non-promoted")
    @Operation(
            summary = "Retrieve all non-promoted advertisements",
            description = "Returns a list of all advertisements that are either not promoted or whose promotion has expired.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Non-promoted advertisements retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = AdResponseDto.class))))
            }
    )
    public ResponseEntity<?> getNonPromotedAds() {
        List<AdResponseDto> ads = adService.getNonPromotedAds();
        return ResponseEntity.status(HttpStatus.OK).body(ads);
    }

    @PostMapping("/{adId}/comments")
    @Operation(
            summary = "Add a comment to an advertisement",
            description = "Adds a new comment to the specified advertisement. The comment is associated with the currently authenticated user.",
            parameters = {
                    @Parameter(name = "adId", description = "Unique identifier of the advertisement", required = true)
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Comment data containing the comment text",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommentCreateDto.class))
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Comment added successfully; returns the updated advertisement",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AdResponseDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid comment data provided",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "BadRequestExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 400,
                                                              "error": "Bad Request",
                                                              "message": "Comment text cannot be empty",
                                                              "path": "/api/ads/11111111-2222-3333-4444-555555555555/comments",
                                                              "method": "POST"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Advertisement or user not found",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "NotFoundExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 404,
                                                              "error": "Not Found",
                                                              "message": "Advertisement not found or user not found",
                                                              "path": "/api/ads/11111111-2222-3333-4444-555555555555/comments",
                                                              "method": "POST"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    public ResponseEntity<?> commentAd(@PathVariable UUID adId,
                                       @Validated @RequestBody @NotNull CommentCreateDto comment) {
        AdResponseDto adResponseDto = adService.commentAd(adId, comment.getCommentText());
        return ResponseEntity.status(HttpStatus.OK).body(adResponseDto);
    }

    @PostMapping(value = "/{adId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload an image for an advertisement",
            description = "Uploads a new image for the specified advertisement. If an image already exists, it is removed first. " +
                    "The authenticated user must be the owner of the advertisement.",
            parameters = {
                    @Parameter(name = "adId", description = "Unique identifier of the advertisement", required = true)
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Multipart form-data containing the image file",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ImageUploadDto.class))
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Image uploaded successfully; returns the updated advertisement",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AdResponseDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid image data provided",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "BadRequestExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 400,
                                                              "error": "Bad Request",
                                                              "message": "Invalid image format",
                                                              "path": "/api/ads/11111111-2222-3333-4444-555555555555/image",
                                                              "method": "POST"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Advertisement not found",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "NotFoundExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 404,
                                                              "error": "Not Found",
                                                              "message": "Advertisement not found",
                                                              "path": "/api/ads/11111111-2222-3333-4444-555555555555/image",
                                                              "method": "POST"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Access denied. The authenticated user is not the owner",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "ForbiddenExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 403,
                                                              "error": "Forbidden",
                                                              "message": "You can only upload images for your own ads",
                                                              "path": "/api/ads/11111111-2222-3333-4444-555555555555/image",
                                                              "method": "POST"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    public ResponseEntity<?> uploadAdImage(@PathVariable UUID adId,
                                           @Validated @ModelAttribute @NotNull ImageUploadDto imageUploadDto) {
        AdResponseDto adResponseDto = adService.uploadAdImage(adId, imageUploadDto.getFile());
        return ResponseEntity.status(HttpStatus.OK).body(adResponseDto);
    }

    @DeleteMapping("/{adId}/image")
    @Operation(
            summary = "Remove the image from an advertisement",
            description = "Removes the existing image from the specified advertisement. " +
                    "Only permitted if the authenticated user is the owner.",
            parameters = {
                    @Parameter(name = "adId", description = "Unique identifier of the advertisement", required = true)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "Image removed successfully"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Advertisement not found",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "NotFoundExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 404,
                                                              "error": "Not Found",
                                                              "message": "Advertisement not found",
                                                              "path": "/api/ads/11111111-2222-3333-4444-555555555555/image",
                                                              "method": "DELETE"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Access denied. The authenticated user is not the owner",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "ForbiddenExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 403,
                                                              "error": "Forbidden",
                                                              "message": "You can only remove images for your own ads",
                                                              "path": "/api/ads/11111111-2222-3333-4444-555555555555/image",
                                                              "method": "DELETE"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    public ResponseEntity<?> removeAdImage(@PathVariable UUID adId) {
        adService.removeAdImage(adId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
