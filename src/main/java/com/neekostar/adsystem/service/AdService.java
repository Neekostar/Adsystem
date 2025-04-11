package com.neekostar.adsystem.service;

import com.neekostar.adsystem.dto.AdCreateDto;
import com.neekostar.adsystem.dto.AdResponseDto;
import com.neekostar.adsystem.dto.AdUpdateDto;
import com.neekostar.adsystem.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface AdService {
    AdResponseDto createAd(AdCreateDto adCreateDto);

    AdResponseDto updateAd(UUID adId, AdUpdateDto adUpdateDto);

    void deleteAd(UUID adId);

    Page<AdResponseDto> getAdsByUser(String username, Pageable pageable);

    AdResponseDto getAd(UUID adId);

    void promoteAd(UUID adId, int days);

    List<AdResponseDto> getPromotedAds();

    List<AdResponseDto> getNonPromotedAds();

    Page<AdResponseDto> filterAds(String city,
                                  Category category,
                                  BigDecimal minPrice,
                                  BigDecimal maxPrice,
                                  String keyword,
                                  String sortBy,
                                  String sortDir,
                                  Pageable pageable);

    AdResponseDto commentAd(UUID adId, String comment);

    AdResponseDto uploadAdImage(UUID adId, MultipartFile file);

    void removeAdImage(UUID adId);
}
