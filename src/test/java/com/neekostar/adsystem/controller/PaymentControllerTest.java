package com.neekostar.adsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neekostar.adsystem.dto.PaymentResponseDto;
import com.neekostar.adsystem.exception.AccessDeniedException;
import com.neekostar.adsystem.exception.GlobalExceptionHandler;
import com.neekostar.adsystem.exception.ResourceNotFoundException;
import com.neekostar.adsystem.service.PaymentService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {
    private static final String API_PAYMENTS = "/api/payments";
    private static final UUID SAMPLE_AD_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final BigDecimal SAMPLE_PAYMENT_AMOUNT = BigDecimal.valueOf(150.00);
    private static final LocalDateTime SAMPLE_PAYMENT_DATE = LocalDateTime.of(2023, 1, 1, 12, 0);
    private static final String TEST_USERNAME = "testuser";

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setControllerAdvice(new GlobalExceptionHandler(messageSource))
                .build();
    }

    @Test
    @DisplayName("POST /api/payments/{adId} - success")
    void createPayment_Success() throws Exception {
        PaymentResponseDto responseDto = buildPaymentResponseDto();
        when(paymentService.createPayment(eq(SAMPLE_AD_ID))).thenReturn(responseDto);

        mockMvc.perform(post(API_PAYMENTS + "/{adId}", SAMPLE_AD_ID)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount", is(SAMPLE_PAYMENT_AMOUNT.doubleValue())))
                .andExpect(jsonPath("$.adId", is(SAMPLE_AD_ID.toString())));
    }

    @Test
    @DisplayName("POST /api/payments/{adId} - ad not found")
    void createPayment_AdNotFound() throws Exception {
        when(paymentService.createPayment(eq(SAMPLE_AD_ID)))
                .thenThrow(new ResourceNotFoundException("Ad", "id", SAMPLE_AD_ID.toString()));

        mockMvc.perform(post(API_PAYMENTS + "/{adId}", SAMPLE_AD_ID)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ad not found with id : '" + SAMPLE_AD_ID + "'"));
    }


    @Test
    @DisplayName("POST /api/payments/{adId} - access denied")
    void createPayment_AccessDenied() throws Exception {
        when(paymentService.createPayment(eq(SAMPLE_AD_ID)))
                .thenThrow(new AccessDeniedException("You can't purchase your own ads"));

        mockMvc.perform(post(API_PAYMENTS + "/{adId}", SAMPLE_AD_ID)
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can't purchase your own ads"));
    }

    @Test
    @DisplayName("GET /api/payments - get payment history success")
    void getPaymentHistory_Success() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TEST_USERNAME, "password")
        );

        Pageable pageable = PageRequest.of(0, 10);
        Page<PaymentResponseDto> history = new PageImpl<>(List.of(buildPaymentResponseDto(), buildPaymentResponseDto()), pageable, 2);
        when(paymentService.getPaymentHistory(eq(TEST_USERNAME), any(Pageable.class))).thenReturn(history);

        mockMvc.perform(get(API_PAYMENTS)
                        .with(csrf())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    private @NotNull PaymentResponseDto buildPaymentResponseDto() {
        PaymentResponseDto dto = new PaymentResponseDto();
        dto.setAmount(SAMPLE_PAYMENT_AMOUNT);
        dto.setAdId(SAMPLE_AD_ID);
        dto.setPaymentDate(SAMPLE_PAYMENT_DATE);
        return dto;
    }
}