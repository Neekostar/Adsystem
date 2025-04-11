package com.neekostar.adsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neekostar.adsystem.dto.ReviewCreateDto;
import com.neekostar.adsystem.dto.ReviewResponseDto;
import com.neekostar.adsystem.exception.AccessDeniedException;
import com.neekostar.adsystem.exception.GlobalExceptionHandler;
import com.neekostar.adsystem.exception.ResourceNotFoundException;
import com.neekostar.adsystem.service.ReviewService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {
    private static final String API_REVIEWS = "/api/reviews";
    private static final String API_REVIEWS_SALE = API_REVIEWS + "/sales/{saleId}";
    private static final String APPLICATION_JSON = "application/json";
    private static final UUID SAMPLE_SALE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final int SAMPLE_RATING_VALUE = 5;
    private static final String SAMPLE_REVIEW_TEXT = "Excellent sale!";
    private static final String SAMPLE_CREATED_AT = "2023-01-01T12:00:00";
    private static final String SAMPLE_BUYER = "buyer1";
    private static final String SAMPLE_SELLER = "seller1";

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController reviewController;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        mockMvc = MockMvcBuilders.standaloneSetup(reviewController)
                .setControllerAdvice(new GlobalExceptionHandler(messageSource))
                .build();
    }

    @Test
    @DisplayName("POST /api/ratings/sales/{saleId} - create rating successfully")
    void createRating_Success() throws Exception {
        ReviewCreateDto createDto = buildReviewCreateDto();
        ReviewResponseDto responseDto = buildReviewResponseDto();

        when(reviewService.createRating(eq(SAMPLE_SALE_ID), eq(createDto))).thenReturn(responseDto);

        mockMvc.perform(post(API_REVIEWS_SALE, SAMPLE_SALE_ID)
                        .contentType(APPLICATION_JSON)
                        .with(csrf())
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is(SAMPLE_BUYER)))
                .andExpect(jsonPath("$.sellerUsername", is(SAMPLE_SELLER)))
                .andExpect(jsonPath("$.saleId", is(SAMPLE_SALE_ID.toString())))
                .andExpect(jsonPath("$.ratingValue", is(SAMPLE_RATING_VALUE)))
                .andExpect(jsonPath("$.reviewText", is(SAMPLE_REVIEW_TEXT)))
                .andExpect(jsonPath("$.createdAt", is(SAMPLE_CREATED_AT)));
    }

    @Test
    @DisplayName("POST /api/ratings/sales/{saleId} - rating creation: resource not found")
    void createRating_ResourceNotFound() throws Exception {
        ReviewCreateDto createDto = buildReviewCreateDto();
        when(reviewService.createRating(eq(SAMPLE_SALE_ID), eq(createDto)))
                .thenThrow(new ResourceNotFoundException("Sale", "id", SAMPLE_SALE_ID.toString()));

        mockMvc.perform(post(API_REVIEWS_SALE, SAMPLE_SALE_ID)
                        .contentType(APPLICATION_JSON)
                        .with(csrf())
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Sale not found with id : '" + SAMPLE_SALE_ID + "'"));
    }

    @Test
    @DisplayName("POST /api/ratings/sales/{saleId} - rating creation: access denied")
    void createRating_AccessDenied() throws Exception {
        ReviewCreateDto createDto = buildReviewCreateDto();
        when(reviewService.createRating(eq(SAMPLE_SALE_ID), eq(createDto)))
                .thenThrow(new AccessDeniedException("You have already rated this sale"));

        mockMvc.perform(post(API_REVIEWS_SALE, SAMPLE_SALE_ID)
                        .contentType(APPLICATION_JSON)
                        .with(csrf())
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You have already rated this sale"));
    }

    @Test
    @DisplayName("POST /api/ratings/sales/{saleId} - rating creation: bad request")
    void createRating_BadRequest() throws Exception {
        ReviewCreateDto createDto = buildReviewCreateDto();
        when(reviewService.createRating(eq(SAMPLE_SALE_ID), eq(createDto)))
                .thenThrow(new IllegalArgumentException("Rating value must be between 1 and 5"));

        mockMvc.perform(post(API_REVIEWS_SALE, SAMPLE_SALE_ID)
                        .contentType(APPLICATION_JSON)
                        .with(csrf())
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Rating value must be between 1 and 5"));
    }

    @Test
    @DisplayName("GET /api/ratings/sales/{saleId} - retrieve ratings successfully")
    void getRatingsBySale_Success() throws Exception {
        ReviewResponseDto dto1 = buildReviewResponseDto();
        ReviewResponseDto dto2 = buildReviewResponseDto();
        List<ReviewResponseDto> ratings = List.of(dto1, dto2);

        when(reviewService.getRatingsBySaleId(SAMPLE_SALE_ID)).thenReturn(ratings);

        mockMvc.perform(get(API_REVIEWS_SALE, SAMPLE_SALE_ID)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/ratings/sales/{saleId} - no ratings found (empty list)")
    void getRatingsBySale_Empty() throws Exception {
        when(reviewService.getRatingsBySaleId(SAMPLE_SALE_ID)).thenReturn(Collections.emptyList());

        mockMvc.perform(get(API_REVIEWS_SALE, SAMPLE_SALE_ID)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    private @NotNull ReviewCreateDto buildReviewCreateDto() {
        ReviewCreateDto dto = new ReviewCreateDto();
        dto.setRatingValue(SAMPLE_RATING_VALUE);
        dto.setReviewText(SAMPLE_REVIEW_TEXT);
        return dto;
    }

    private @NotNull ReviewResponseDto buildReviewResponseDto() {
        ReviewResponseDto dto = new ReviewResponseDto();
        dto.setUsername(SAMPLE_BUYER);
        dto.setSellerUsername(SAMPLE_SELLER);
        dto.setSaleId(SAMPLE_SALE_ID);
        dto.setRatingValue(SAMPLE_RATING_VALUE);
        dto.setReviewText(SAMPLE_REVIEW_TEXT);
        dto.setCreatedAt(SAMPLE_CREATED_AT);
        return dto;
    }
}