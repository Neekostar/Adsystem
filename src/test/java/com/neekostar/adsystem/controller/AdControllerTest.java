package com.neekostar.adsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neekostar.adsystem.dto.AdCreateDto;
import com.neekostar.adsystem.dto.AdResponseDto;
import com.neekostar.adsystem.dto.AdUpdateDto;
import com.neekostar.adsystem.dto.ImageUploadDto;
import com.neekostar.adsystem.exception.AccessDeniedException;
import com.neekostar.adsystem.exception.GlobalExceptionHandler;
import com.neekostar.adsystem.exception.ResourceNotFoundException;
import com.neekostar.adsystem.model.Category;
import com.neekostar.adsystem.service.AdService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdControllerTest {

    private static final String API_ADS = "/api/ads";
    private static final String APPLICATION_JSON = "application/json";
    private static final String MOSCOW = "Moscow";
    private static final String CATEGORY_VEHICLES = Category.VEHICLES.name();
    private static final String NEW_AD_TITLE = "New Ad Title";
    private static final String NEW_AD_DESCRIPTION = "New Ad Description";
    private static final BigDecimal DEFAULT_PRICE = BigDecimal.valueOf(100);
    private static final String UPDATED_TITLE = "Updated Title";
    private static final BigDecimal UPDATED_PRICE = BigDecimal.valueOf(999);

    private MockMvc mockMvc;

    @Mock
    private AdService adService;

    @InjectMocks
    private AdController adController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        mockMvc = MockMvcBuilders.standaloneSetup(adController)
                .setControllerAdvice(new GlobalExceptionHandler(messageSource))
                .build();
    }

    @Test
    @DisplayName("POST /api/ads - unauthorized should fail")
    void createAd_Unauthorized() throws Exception {
        AdCreateDto createDto = buildAdCreateDto(true);
        when(adService.createAd(any())).thenThrow(new AccessDeniedException("User is not authorized"));

        mockMvc.perform(post(API_ADS)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/ads - success with authorized user")
    void createAd_Success() throws Exception {
        UUID generatedId = UUID.randomUUID();
        AdResponseDto responseDto = new AdResponseDto();
        responseDto.setId(generatedId);
        responseDto.setTitle(NEW_AD_TITLE);

        when(adService.createAd(any())).thenReturn(responseDto);

        AdCreateDto createDto = buildAdCreateDto(false);

        mockMvc.perform(post(API_ADS)
                        .contentType(APPLICATION_JSON)
                        .with(csrf())
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(generatedId.toString()))
                .andExpect(jsonPath("$.title").value(NEW_AD_TITLE));
    }

    @Test
    @DisplayName("GET /api/ads/{adId} - success")
    void getAd_Success() throws Exception {
        UUID adId = UUID.randomUUID();
        AdResponseDto dto = new AdResponseDto();
        dto.setId(adId);
        dto.setTitle("Existing Ad");

        when(adService.getAd(adId)).thenReturn(dto);

        mockMvc.perform(get(API_ADS + "/{adId}", adId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(adId.toString()))
                .andExpect(jsonPath("$.title").value("Existing Ad"));
    }

    @Test
    @DisplayName("GET /api/ads/{adId} - not found")
    void getAd_NotFound() throws Exception {
        UUID wrongId = UUID.randomUUID();
        when(adService.getAd(wrongId))
                .thenThrow(new ResourceNotFoundException("Ad", "id", wrongId.toString()));

        mockMvc.perform(get(API_ADS + "/{id}", wrongId)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/ads/{adId} - update success")
    void updateAd_Success() throws Exception {
        UUID adId = UUID.randomUUID();
        AdResponseDto dto = new AdResponseDto();
        dto.setId(adId);
        dto.setTitle(UPDATED_TITLE);

        when(adService.updateAd(eq(adId), any())).thenReturn(dto);

        AdUpdateDto updateDto = buildAdUpdateDto();

        mockMvc.perform(put(API_ADS + "/{id}", adId)
                        .contentType(APPLICATION_JSON)
                        .with(csrf())
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(UPDATED_TITLE));
    }

    @Test
    @DisplayName("DELETE /api/ads/{adId} - success => 204 no content")
    void deleteAd_Success() throws Exception {
        UUID adId = UUID.randomUUID();

        mockMvc.perform(delete(API_ADS + "/{id}", adId)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(adService).deleteAd(adId);
    }

    @Test
    @DisplayName("GET /api/ads - filter ads returns 200 OK")
    void getAllAds_Success() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AdResponseDto> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(adService.filterAds(
                eq(MOSCOW),
                eq(Category.valueOf(CATEGORY_VEHICLES)),
                any(BigDecimal.class),
                any(BigDecimal.class),
                eq("car"),
                eq("price"),
                eq("asc"),
                any(Pageable.class)
        )).thenReturn(emptyPage);

        mockMvc.perform(get(API_ADS)
                        .with(csrf())
                        .param("city", MOSCOW)
                        .param("category", CATEGORY_VEHICLES)
                        .param("minPrice", "0")
                        .param("maxPrice", "1000")
                        .param("keyword", "car")
                        .param("sortBy", "price")
                        .param("sortDir", "asc")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/ads/user/{username} - success")
    void getAdsByUser_Success() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AdResponseDto> page = new PageImpl<>(List.of(new AdResponseDto(), new AdResponseDto()), pageable, 2);
        when(adService.getAdsByUser(eq("johndoe"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get(API_ADS + "/user/{username}", "johndoe")
                        .param("page", "0")
                        .param("size", "10")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    @DisplayName("POST /api/ads/{adId}/promote - success => 200")
    void promoteAd_Success() throws Exception {
        UUID adId = UUID.randomUUID();

        mockMvc.perform(post(API_ADS + "/{id}/promote", adId)
                        .with(csrf())
                        .param("days", "7"))
                .andExpect(status().isOk());

        verify(adService).promoteAd(adId, 7);
    }

    @Test
    @DisplayName("GET /api/ads/promoted => 200")
    void getPromotedAds_Success() throws Exception {
        when(adService.getPromotedAds())
                .thenReturn(List.of(new AdResponseDto(), new AdResponseDto()));

        mockMvc.perform(get(API_ADS + "/promoted")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/ads/non-promoted => 200")
    void getNonPromotedAds_Success() throws Exception {
        when(adService.getNonPromotedAds())
                .thenReturn(List.of());

        mockMvc.perform(get(API_ADS + "/non-promoted")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("POST /api/ads/{adId}/comments - success")
    void commentAd_Success() throws Exception {
        UUID adId = UUID.randomUUID();
        AdResponseDto dto = new AdResponseDto();
        dto.setId(adId);
        dto.setTitle("Ad with comment");

        String commentText = "Great ad!";
        when(adService.commentAd(eq(adId), eq(commentText))).thenReturn(dto);

        String jsonComment = "{\"commentText\": \"" + commentText + "\"}";

        mockMvc.perform(post(API_ADS + "/{adId}/comments", adId)
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(jsonComment))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(adId.toString()));
    }

    @Test
    @DisplayName("POST /api/ads/{adId}/image - success")
    void uploadAdImage_Success() throws Exception {
        UUID adId = UUID.randomUUID();
        AdResponseDto dto = new AdResponseDto();
        dto.setId(adId);
        dto.setTitle("Ad with image");

        byte[] imageContent = "fake image content".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg", imageContent);
        ImageUploadDto imageUploadDto = new ImageUploadDto();
        imageUploadDto.setFile(file);

        when(adService.uploadAdImage(eq(adId), any())).thenReturn(dto);

        mockMvc.perform(multipart(API_ADS + "/{adId}/image", adId)
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(adId.toString()));
    }

    @Test
    @DisplayName("DELETE /api/ads/{adId}/image - success")
    void removeAdImage_Success() throws Exception {
        UUID adId = UUID.randomUUID();

        mockMvc.perform(delete(API_ADS + "/{adId}/image", adId)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(adService).removeAdImage(adId);
    }

    private @NotNull AdCreateDto buildAdCreateDto(boolean includeDescription) {
        AdCreateDto dto = new AdCreateDto();
        dto.setTitle(NEW_AD_TITLE);
        if (includeDescription) {
            dto.setDescription(NEW_AD_DESCRIPTION);
        }
        dto.setPrice(DEFAULT_PRICE);
        dto.setCity(MOSCOW);
        dto.setCategory(CATEGORY_VEHICLES);
        return dto;
    }

    private @NotNull AdUpdateDto buildAdUpdateDto() {
        AdUpdateDto dto = new AdUpdateDto();
        dto.setTitle(UPDATED_TITLE);
        dto.setPrice(UPDATED_PRICE);
        return dto;
    }
}
