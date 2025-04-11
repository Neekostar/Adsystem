package com.neekostar.adsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neekostar.adsystem.dto.SaleHistoryResponseDto;
import com.neekostar.adsystem.exception.GlobalExceptionHandler;
import com.neekostar.adsystem.service.SaleHistoryService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ExtendWith(MockitoExtension.class)
class SaleHistoryControllerTest {
    private static final String API_SALES = "/api/sales";
    private static final String API_SALES_SELLER = API_SALES + "/seller/{sellerUsername}";
    private static final String API_SALES_PURCHASES = API_SALES + "/purchases";
    private static final String APPLICATION_JSON = "application/json";
    private static final String SAMPLE_SELLER_USERNAME = "seller1";
    private static final String SAMPLE_BUYER_USERNAME = "buyer1";
    private static final UUID SAMPLE_SALE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String SAMPLE_AD_TITLE = "Amazing Car";
    private static final LocalDateTime SAMPLE_SALE_DATE = LocalDateTime.of(2023, 1, 1, 12, 0);

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SaleHistoryService saleHistoryService;

    @InjectMocks
    private SaleHistoryController saleHistoryController;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        mockMvc = MockMvcBuilders.standaloneSetup(saleHistoryController)
                .setControllerAdvice(new GlobalExceptionHandler(messageSource))
                .build();
    }

    @Test
    @DisplayName("GET /api/sales/seller/{sellerUsername} - success")
    void getSalesBySeller_Success() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        List<SaleHistoryResponseDto> salesList = List.of(buildSaleHistoryResponseDto(), buildSaleHistoryResponseDto());
        Page<SaleHistoryResponseDto> page = new PageImpl<>(salesList, pageable, salesList.size());
        when(saleHistoryService.getSalesBySeller(eq(SAMPLE_SELLER_USERNAME), eq(pageable))).thenReturn(page);

        mockMvc.perform(get(API_SALES_SELLER, SAMPLE_SELLER_USERNAME)
                        .with(csrf())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].sellerUsername", is(SAMPLE_SELLER_USERNAME)))
                .andExpect(jsonPath("$.content[0].adTitle", is(SAMPLE_AD_TITLE)));
    }

    @Test
    @DisplayName("GET /api/sales/seller/{sellerUsername} - invalid seller username (blank) returns 400")
    void getSalesBySeller_InvalidSellerUsername() throws Exception {
        String invalidSeller = " ";
        when(saleHistoryService.getSalesBySeller(eq(invalidSeller), any(Pageable.class)))
                .thenThrow(new IllegalArgumentException("Invalid seller username"));

        mockMvc.perform(get(API_SALES_SELLER, invalidSeller)
                        .with(csrf())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid seller username"));
    }

    @Test
    @DisplayName("GET /api/sales/purchases - success")
    void getPurchasesByBuyer_Success() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(SAMPLE_BUYER_USERNAME, "password")
        );

        Pageable pageable = PageRequest.of(0, 10);
        List<SaleHistoryResponseDto> purchasesList = List.of(buildSaleHistoryResponseDto());
        Page<SaleHistoryResponseDto> page = new PageImpl<>(purchasesList, pageable, purchasesList.size());
        when(saleHistoryService.getPurchasesByBuyer(eq(SAMPLE_BUYER_USERNAME), eq(pageable))).thenReturn(page);

        mockMvc.perform(get(API_SALES_PURCHASES)
                        .with(csrf())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].buyerUsername", is(SAMPLE_BUYER_USERNAME)));
    }

    @Test
    @DisplayName("GET /api/sales/purchases - invalid buyer username returns 400")
    void getPurchasesByBuyer_InvalidUsername() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("", "password")
        );

        Pageable pageable = PageRequest.of(0, 10);
        when(saleHistoryService.getPurchasesByBuyer(eq(""), eq(pageable)))
                .thenThrow(new IllegalArgumentException("Invalid buyer username"));

        mockMvc.perform(get(API_SALES_PURCHASES)
                        .with(csrf())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid buyer username"));
    }

    private @NotNull SaleHistoryResponseDto buildSaleHistoryResponseDto() {
        SaleHistoryResponseDto dto = new SaleHistoryResponseDto();
        dto.setSaleId(SAMPLE_SALE_ID);
        dto.setSellerUsername(SAMPLE_SELLER_USERNAME);
        dto.setAd(null);
        dto.setAdTitle(SAMPLE_AD_TITLE);
        dto.setBuyerUsername(SAMPLE_BUYER_USERNAME);
        dto.setSaleDate(SAMPLE_SALE_DATE);
        return dto;
    }
}