package com.neekostar.adsystem.service.impl;

import com.neekostar.adsystem.dto.AdCreateDto;
import com.neekostar.adsystem.dto.AdResponseDto;
import com.neekostar.adsystem.dto.AdUpdateDto;
import com.neekostar.adsystem.exception.AccessDeniedException;
import com.neekostar.adsystem.exception.InvalidArgumentException;
import com.neekostar.adsystem.exception.ResourceNotFoundException;
import com.neekostar.adsystem.mapper.AdMapper;
import com.neekostar.adsystem.model.*;
import com.neekostar.adsystem.repository.AdRepository;
import com.neekostar.adsystem.repository.CommentRepository;
import com.neekostar.adsystem.repository.UserRepository;
import com.neekostar.adsystem.service.MinioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AdServiceImplTest {

    private static final String CURRENT_USER = "testUser";
    private static final String ANOTHER_USER = "anotherUser";
    private static final String MOSCOW = "Moscow";
    private static final String VEHICLES = "VEHICLES";
    private static final String UPDATED_TITLE = "Updated Title";
    private static final String UPDATED_DESCRIPTION = "Updated Description";
    private static final String NEW_AD = "New Ad";
    private static final String IMAGE_URL = "http://localhost:9000/bucket-name/ads/image.jpg";
    private static final String MINIO_BUCKET = "ads";
    private static final String MINIO_OBJECT_NAME = "ads/image.jpg";
    private static final String SOME_URL = "http://some.url";
    private static final BigDecimal PRICE_1000 = BigDecimal.valueOf(1000);
    private static final BigDecimal PRICE_500 = BigDecimal.valueOf(500);
    private static final BigDecimal PRICE_1500 = BigDecimal.valueOf(1500);
    private static final BigDecimal PRICE_2000 = BigDecimal.valueOf(2000);

    @Mock
    private AdRepository adRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private AdMapper adMapper;
    @Mock
    private MinioService minioService;
    @Mock
    private Authentication authentication;
    @InjectMocks
    private AdServiceImpl adService;

    private User testUser;
    private Ad testAd;
    private AdResponseDto testAdResponseDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername(CURRENT_USER);
        testUser.setRating(4.5f);

        testAd = new Ad();
        testAd.setId(UUID.randomUUID());
        testAd.setTitle(NEW_AD);
        testAd.setPrice(PRICE_1000);
        testAd.setUser(testUser);
        testAd.setStatus(AdStatus.ACTIVE);
        testAd.setComments(new ArrayList<>());

        testAdResponseDto = new AdResponseDto();
        testAdResponseDto.setId(testAd.getId());
        testAdResponseDto.setTitle(testAd.getTitle());
        testAdResponseDto.setUsername(CURRENT_USER);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setupSecurityContext() {
        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
        when(authentication.getName()).thenReturn(CURRENT_USER);
    }

    @Test
    void createAd_Success() {
        setupSecurityContext();
        AdCreateDto createDto = new AdCreateDto();
        createDto.setTitle(NEW_AD);
        createDto.setPrice(PRICE_500);
        createDto.setCity(MOSCOW);
        createDto.setCategory(VEHICLES);

        when(userRepository.findUserByUsername(CURRENT_USER)).thenReturn(Optional.of(testUser));
        when(adMapper.toEntity(createDto)).thenReturn(testAd);
        when(adRepository.save(testAd)).thenReturn(testAd);
        when(adMapper.toDto(testAd)).thenReturn(testAdResponseDto);

        AdResponseDto result = adService.createAd(createDto);

        assertNotNull(result);
        assertEquals(testAd.getId(), result.getId());
        verify(adRepository).save(testAd);
    }

    @Test
    void createAd_UserNotFound() {
        setupSecurityContext();
        AdCreateDto createDto = new AdCreateDto();
        when(userRepository.findUserByUsername(CURRENT_USER)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adService.createAd(createDto));
        verify(adRepository, never()).save(any());
    }

    @Test
    void updateAd_Success() {
        setupSecurityContext();
        AdUpdateDto updateDto = new AdUpdateDto();
        updateDto.setTitle(UPDATED_TITLE);
        updateDto.setPrice(PRICE_1500);

        when(adRepository.findById(testAd.getId())).thenReturn(Optional.of(testAd));
        when(adRepository.save(testAd)).thenReturn(testAd);
        when(adMapper.toDto(testAd)).thenReturn(testAdResponseDto);

        AdResponseDto result = adService.updateAd(testAd.getId(), updateDto);

        assertEquals(UPDATED_TITLE, testAd.getTitle());
        assertEquals(PRICE_1500, testAd.getPrice());
        verify(adRepository).save(testAd);
        assertNotNull(result);
    }

    @Test
    void updateAd_UpdateDescriptionOnly() {
        setupSecurityContext();
        AdUpdateDto updateDto = new AdUpdateDto();
        updateDto.setDescription(UPDATED_DESCRIPTION);

        when(adRepository.findById(testAd.getId())).thenReturn(Optional.of(testAd));
        when(adRepository.save(testAd)).thenReturn(testAd);
        when(adMapper.toDto(testAd)).thenReturn(testAdResponseDto);

        adService.updateAd(testAd.getId(), updateDto);

        assertEquals(UPDATED_DESCRIPTION, testAd.getDescription());
        verify(adRepository).save(testAd);
    }

    @Test
    void updateAd_AdNotFound() {
        setupSecurityContext();
        UUID wrongId = UUID.randomUUID();

        when(adRepository.findById(wrongId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adService.updateAd(wrongId, new AdUpdateDto()));
    }

    @Test
    void updateAd_AccessDenied() {
        setupSecurityContext();
        Ad anotherUserAd = new Ad();
        anotherUserAd.setId(UUID.randomUUID());
        User anotherUser = new User();
        anotherUser.setUsername(ANOTHER_USER);
        anotherUserAd.setUser(anotherUser);

        when(adRepository.findById(anotherUserAd.getId())).thenReturn(Optional.of(anotherUserAd));

        assertThrows(AccessDeniedException.class, () -> adService.updateAd(anotherUserAd.getId(), new AdUpdateDto()));
        verify(adRepository, never()).save(any());
    }

    @Test
    void deleteAd_Success() {
        setupSecurityContext();
        when(adRepository.findById(testAd.getId())).thenReturn(Optional.of(testAd));

        adService.deleteAd(testAd.getId());

        verify(commentRepository).deleteByAdId(testAd.getId());
        verify(adRepository).delete(testAd);
    }

    @Test
    void deleteAd_WithComments() {
        setupSecurityContext();
        testAd.setComments(List.of(new Comment(), new Comment()));
        when(adRepository.findById(testAd.getId())).thenReturn(Optional.of(testAd));

        adService.deleteAd(testAd.getId());

        verify(commentRepository).deleteByAdId(testAd.getId());
        verify(adRepository).delete(testAd);
    }

    @Test
    void deleteAd_NotFound() {
        setupSecurityContext();
        UUID wrongId = UUID.randomUUID();
        when(adRepository.findById(wrongId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adService.deleteAd(wrongId));
    }

    @Test
    void deleteAd_AccessDenied() {
        setupSecurityContext();
        Ad otherAd = new Ad();
        otherAd.setId(UUID.randomUUID());
        User anotherUser = new User();
        anotherUser.setUsername(ANOTHER_USER);
        otherAd.setUser(anotherUser);

        when(adRepository.findById(otherAd.getId())).thenReturn(Optional.of(otherAd));

        assertThrows(AccessDeniedException.class, () -> adService.deleteAd(otherAd.getId()));
        verify(adRepository, never()).delete(any());
    }

    @Test
    void getAdsByUser_WithPageable_Success() {
        setupSecurityContext();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Ad> page = new PageImpl<>(List.of(testAd), pageable, 1);
        when(adRepository.findAdByUserUsernameAndStatus(CURRENT_USER, AdStatus.ACTIVE, pageable)).thenReturn(page);
        when(adMapper.toDto(testAd)).thenReturn(testAdResponseDto);

        Page<AdResponseDto> result = adService.getAdsByUser(CURRENT_USER, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(adRepository).findAdByUserUsernameAndStatus(CURRENT_USER, AdStatus.ACTIVE, pageable);
    }

    @Test
    void getAd_Success() {
        when(adRepository.findById(testAd.getId())).thenReturn(Optional.of(testAd));
        when(adMapper.toDto(testAd)).thenReturn(testAdResponseDto);

        AdResponseDto result = adService.getAd(testAd.getId());

        assertNotNull(result);
        assertEquals(testAd.getId(), result.getId());
    }

    @Test
    void getAd_NotFound() {
        UUID wrongId = UUID.randomUUID();
        when(adRepository.findById(wrongId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adService.getAd(wrongId));
    }

    @Test
    void promoteAd_Success() {
        setupSecurityContext();
        when(adRepository.findById(testAd.getId())).thenReturn(Optional.of(testAd));

        adService.promoteAd(testAd.getId(), 7);

        assertTrue(testAd.getIsPromoted());
        assertNotNull(testAd.getPromotionEndDate());
        verify(adRepository).save(testAd);
    }

    @Test
    void promoteAd_AdNotFound() {
        setupSecurityContext();
        UUID wrongId = UUID.randomUUID();
        when(adRepository.findById(wrongId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adService.promoteAd(wrongId, 5));
    }

    @Test
    void promoteAd_NotOwner() {
        setupSecurityContext();
        Ad ad2 = new Ad();
        ad2.setId(UUID.randomUUID());
        User anotherUser = new User();
        anotherUser.setUsername(ANOTHER_USER);
        ad2.setUser(anotherUser);

        when(adRepository.findById(ad2.getId())).thenReturn(Optional.of(ad2));

        assertThrows(AccessDeniedException.class, () -> adService.promoteAd(ad2.getId(), 5));
        verify(adRepository, never()).save(any());
    }

    @Test
    void promoteAd_InvalidDays() {
        setupSecurityContext();
        when(adRepository.findById(testAd.getId())).thenReturn(Optional.of(testAd));

        assertThrows(InvalidArgumentException.class, () -> adService.promoteAd(testAd.getId(), 0));
        assertThrows(InvalidArgumentException.class, () -> adService.promoteAd(testAd.getId(), -3));
        verify(adRepository, never()).save(any());
    }

    @Test
    void getPromotedAds_ActiveOnly() {
        Ad promotedAd = new Ad();
        promotedAd.setId(UUID.randomUUID());
        promotedAd.setIsPromoted(true);
        promotedAd.setPromotionEndDate(LocalDateTime.now().plusDays(1));
        promotedAd.setStatus(AdStatus.ACTIVE);
        when(adMapper.toDto(promotedAd)).thenReturn(new AdResponseDto());

        when(adRepository.findAll()).thenReturn(List.of(promotedAd, testAd));

        List<AdResponseDto> result = adService.getPromotedAds();

        assertEquals(1, result.size());
    }

    @Test
    void getNonPromotedAds_ActiveOnly() {
        Ad promotedAd = new Ad();
        promotedAd.setIsPromoted(true);
        promotedAd.setPromotionEndDate(LocalDateTime.now().plusDays(2));
        promotedAd.setStatus(AdStatus.ACTIVE);

        Ad expiredAd = new Ad();
        expiredAd.setIsPromoted(true);
        expiredAd.setPromotionEndDate(LocalDateTime.now().minusDays(1));
        expiredAd.setStatus(AdStatus.ACTIVE);

        Ad normalAd = new Ad();
        normalAd.setIsPromoted(false);
        normalAd.setStatus(AdStatus.ACTIVE);

        when(adRepository.findAll()).thenReturn(List.of(promotedAd, expiredAd, normalAd));
        when(adMapper.toDto(any())).thenReturn(new AdResponseDto());

        List<AdResponseDto> result = adService.getNonPromotedAds();
        assertEquals(2, result.size());
    }

    @Test
    void filterAds_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Ad filteredAd = new Ad();
        filteredAd.setId(UUID.randomUUID());
        filteredAd.setIsPromoted(true);
        filteredAd.setPromotionEndDate(LocalDateTime.now().plusDays(1));
        filteredAd.setStatus(AdStatus.ACTIVE);
        filteredAd.setUser(testUser);

        when(adRepository.findAll(ArgumentMatchers.<Specification<Ad>>any(), any(Sort.class)))
                .thenReturn(List.of(filteredAd, testAd));
        when(adMapper.toDto(any(Ad.class))).thenReturn(new AdResponseDto());

        Page<AdResponseDto> result = adService.filterAds(
                MOSCOW, Category.VEHICLES,
                PRICE_500, PRICE_2000,
                "test", "price", "asc", pageable
        );

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        verify(adRepository).findAll(ArgumentMatchers.<Specification<Ad>>any(), any(Sort.class));
    }

    @Test
    void filterAds_InvalidSortField() {
        Pageable pageable = PageRequest.of(0, 10);
        Ad ad1 = new Ad();
        ad1.setId(UUID.randomUUID());
        ad1.setIsPromoted(false);
        ad1.setStatus(AdStatus.ACTIVE);
        ad1.setUser(testUser);
        when(adRepository.findAll(ArgumentMatchers.<Specification<Ad>>any(), any(Sort.class)))
                .thenReturn(List.of(ad1));
        when(adMapper.toDto(ad1)).thenReturn(new AdResponseDto());

        Page<AdResponseDto> result = adService.filterAds(
                MOSCOW, Category.VEHICLES,
                PRICE_500, PRICE_2000,
                "test", "invalid", "desc", pageable
        );
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void filterAds_DescendingSort() {
        Pageable pageable = PageRequest.of(0, 10);
        Ad ad1 = new Ad();
        ad1.setId(UUID.randomUUID());
        ad1.setIsPromoted(false);
        ad1.setStatus(AdStatus.ACTIVE);
        ad1.setUser(testUser);
        when(adRepository.findAll(ArgumentMatchers.<Specification<Ad>>any(), any(Sort.class)))
                .thenReturn(List.of(ad1));
        when(adMapper.toDto(ad1)).thenReturn(new AdResponseDto());

        Page<AdResponseDto> result = adService.filterAds(
                MOSCOW, Category.VEHICLES,
                PRICE_500, PRICE_2000,
                "test", "price", "desc", pageable
        );
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void filterAds_EmptyPage() {
        Pageable pageable = PageRequest.of(1, 10);
        when(adRepository.findAll(ArgumentMatchers.<Specification<Ad>>any(), any(Sort.class)))
                .thenReturn(List.of(testAd));
        Page<AdResponseDto> result = adService.filterAds(
                MOSCOW, Category.VEHICLES,
                PRICE_500, PRICE_2000,
                "test", "price", "asc", pageable
        );
        assertEquals(0, result.getNumberOfElements());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void filterAds_NoActiveAds() {
        Pageable pageable = PageRequest.of(0, 10);
        Ad adInactive = new Ad();
        adInactive.setId(UUID.randomUUID());
        adInactive.setStatus(AdStatus.SOLD);
        adInactive.setUser(testUser);
        when(adRepository.findAll(ArgumentMatchers.<Specification<Ad>>any(), any(Sort.class)))
                .thenReturn(List.of(adInactive));
        Page<AdResponseDto> result = adService.filterAds(
                MOSCOW, Category.VEHICLES,
                PRICE_500, PRICE_2000,
                "test", "price", "asc", pageable
        );
        assertEquals(0, result.getNumberOfElements());
    }

    @Test
    void filterAds_SortingByUserRating() {
        Pageable pageable = PageRequest.of(0, 10);
        Ad adLowRating = new Ad();
        adLowRating.setId(UUID.randomUUID());
        adLowRating.setIsPromoted(false);
        adLowRating.setStatus(AdStatus.ACTIVE);
        User userLow = new User();
        userLow.setRating(2.0f);
        adLowRating.setUser(userLow);

        Ad adHighRating = new Ad();
        adHighRating.setId(UUID.randomUUID());
        adHighRating.setIsPromoted(false);
        adHighRating.setStatus(AdStatus.ACTIVE);
        User userHigh = new User();
        userHigh.setRating(5.0f);
        adHighRating.setUser(userHigh);

        when(adRepository.findAll(ArgumentMatchers.<Specification<Ad>>any(), any(Sort.class)))
                .thenReturn(List.of(adLowRating, adHighRating));
        when(adMapper.toDto(any(Ad.class))).thenReturn(new AdResponseDto());

        Page<AdResponseDto> result = adService.filterAds(
                MOSCOW, Category.VEHICLES,
                PRICE_500, PRICE_2000,
                "test", "price", "asc", pageable
        );
        assertEquals(2, result.getTotalElements());
    }

    @Test
    void commentAd_Success() {
        setupSecurityContext();
        UUID adId = testAd.getId();
        String commentText = "Great ad!";

        when(adRepository.findById(adId)).thenReturn(Optional.of(testAd));
        when(userRepository.findUserByUsername(CURRENT_USER)).thenReturn(Optional.of(testUser));
        when(commentRepository.saveAndFlush(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(UUID.randomUUID());
            return comment;
        });
        when(adRepository.save(any(Ad.class))).thenReturn(testAd);
        when(adMapper.toDto(testAd)).thenReturn(testAdResponseDto);

        AdResponseDto result = adService.commentAd(adId, commentText);

        assertNotNull(result);
        verify(commentRepository).saveAndFlush(any(Comment.class));
        verify(adRepository).save(testAd);

        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).saveAndFlush(commentCaptor.capture());
        Comment savedComment = commentCaptor.getValue();
        assertEquals(commentText, savedComment.getCommentText());
        assertEquals(testUser, savedComment.getUser());
        assertEquals(testAd, savedComment.getAd());
    }

    @Test
    void commentAd_AdNotFound() {
        setupSecurityContext();
        when(adRepository.findById(testAd.getId())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> adService.commentAd(testAd.getId(), "Some comment"));
    }

    @Test
    void commentAd_UserNotFound() {
        setupSecurityContext();
        when(adRepository.findById(testAd.getId())).thenReturn(Optional.of(testAd));
        when(userRepository.findUserByUsername(CURRENT_USER)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adService.commentAd(testAd.getId(), "Comment"));
    }

    @Test
    void uploadAdImage_Success() {
        setupSecurityContext();
        MultipartFile mockFile = mock(MultipartFile.class);

        when(adRepository.findById(testAd.getId())).thenReturn(Optional.of(testAd));
        when(minioService.uploadFile(mockFile, MINIO_BUCKET)).thenReturn(SOME_URL);
        when(adRepository.saveAndFlush(testAd)).thenReturn(testAd);
        when(adMapper.toDto(testAd)).thenReturn(testAdResponseDto);

        AdResponseDto dto = adService.uploadAdImage(testAd.getId(), mockFile);
        assertNotNull(dto);
        verify(minioService).uploadFile(mockFile, MINIO_BUCKET);
        verify(adRepository).saveAndFlush(testAd);
    }

    @Test
    void uploadAdImage_AdNotFound() {
        setupSecurityContext();
        MultipartFile mockFile = mock(MultipartFile.class);
        when(adRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adService.uploadAdImage(UUID.randomUUID(), mockFile));
        verify(minioService, never()).uploadFile(any(), any());
    }

    @Test
    void uploadAdImage_WithExistingImage_ShouldRemoveOldImageAndUploadNew() {
        setupSecurityContext();
        MultipartFile mockFile = mock(MultipartFile.class);
        String existingImageUrl = "http://old.image.url/image.jpg";
        testAd.setImageUrl(existingImageUrl);
        String resolvedObjectName = "image.jpg";

        when(adRepository.findById(testAd.getId())).thenReturn(Optional.of(testAd));
        when(minioService.resolveObjectNameFromUrl(existingImageUrl)).thenReturn(resolvedObjectName);
        doNothing().when(minioService).removeFile(resolvedObjectName);
        when(minioService.uploadFile(mockFile, "ads")).thenReturn(SOME_URL);
        when(adRepository.saveAndFlush(testAd)).thenReturn(testAd);
        when(adMapper.toDto(testAd)).thenReturn(testAdResponseDto);

        AdResponseDto dto = adService.uploadAdImage(testAd.getId(), mockFile);
        assertNotNull(dto);
        verify(minioService).resolveObjectNameFromUrl(existingImageUrl);
        verify(minioService).removeFile(resolvedObjectName);
        verify(minioService).uploadFile(mockFile, "ads");
        verify(adRepository).saveAndFlush(testAd);
    }


    @Test
    void uploadAdImage_AccessDenied() {
        setupSecurityContext();
        MultipartFile mockFile = mock(MultipartFile.class);

        Ad otherAd = new Ad();
        otherAd.setId(UUID.randomUUID());
        User anotherUser = new User();
        anotherUser.setUsername(ANOTHER_USER);
        otherAd.setUser(anotherUser);

        when(adRepository.findById(otherAd.getId())).thenReturn(Optional.of(otherAd));

        assertThrows(AccessDeniedException.class, () -> adService.uploadAdImage(otherAd.getId(), mockFile));
        verify(minioService, never()).uploadFile(any(), any());
    }

    @Test
    void removeAdImage_Success() {
        setupSecurityContext();
        testAd.setImageUrl(IMAGE_URL);

        when(adRepository.findById(testAd.getId())).thenReturn(Optional.of(testAd));
        when(minioService.resolveObjectNameFromUrl(IMAGE_URL)).thenReturn(MINIO_OBJECT_NAME);
        doNothing().when(minioService).removeFile(anyString());

        adService.removeAdImage(testAd.getId());

        verify(minioService).resolveObjectNameFromUrl(IMAGE_URL);
        verify(minioService).removeFile(MINIO_OBJECT_NAME);
        assertNull(testAd.getImageUrl());
    }

    @Test
    void removeAdImage_AdNotFound() {
        setupSecurityContext();
        when(adRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adService.removeAdImage(UUID.randomUUID()));
        verify(minioService, never()).removeFile(anyString());
    }

    @Test
    void removeAdImage_AccessDenied() {
        setupSecurityContext();
        Ad otherAd = new Ad();
        otherAd.setId(UUID.randomUUID());
        User anotherUser = new User();
        anotherUser.setUsername(ANOTHER_USER);
        otherAd.setUser(anotherUser);

        when(adRepository.findById(otherAd.getId())).thenReturn(Optional.of(otherAd));
        assertThrows(AccessDeniedException.class, () -> adService.removeAdImage(otherAd.getId()));
        verify(minioService, never()).removeFile(anyString());
    }

    @Test
    void removeAdImage_NoImage() {
        setupSecurityContext();
        testAd.setImageUrl(null);
        when(adRepository.findById(testAd.getId())).thenReturn(Optional.of(testAd));

        adService.removeAdImage(testAd.getId());

        verify(minioService, never()).removeFile(anyString());
        verify(adRepository, never()).saveAndFlush(testAd);
        assertNull(testAd.getImageUrl());
    }
}
