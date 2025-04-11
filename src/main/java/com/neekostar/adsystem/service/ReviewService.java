package com.neekostar.adsystem.service;

import com.neekostar.adsystem.dto.ReviewCreateDto;
import com.neekostar.adsystem.dto.ReviewResponseDto;

import java.util.List;
import java.util.UUID;

public interface ReviewService {
    ReviewResponseDto createRating(UUID saleId, ReviewCreateDto reviewCreateDto);

    List<ReviewResponseDto> getRatingsBySaleId(UUID saleId);
}
