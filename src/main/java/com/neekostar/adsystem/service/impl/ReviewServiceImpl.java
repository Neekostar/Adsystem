package com.neekostar.adsystem.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import com.neekostar.adsystem.dto.ReviewCreateDto;
import com.neekostar.adsystem.dto.ReviewResponseDto;
import com.neekostar.adsystem.exception.AccessDeniedException;
import com.neekostar.adsystem.exception.ResourceNotFoundException;
import com.neekostar.adsystem.mapper.ReviewMapper;
import com.neekostar.adsystem.model.Review;
import com.neekostar.adsystem.model.SaleHistory;
import com.neekostar.adsystem.model.User;
import com.neekostar.adsystem.repository.ReviewRepository;
import com.neekostar.adsystem.repository.SaleHistoryRepository;
import com.neekostar.adsystem.repository.UserRepository;
import com.neekostar.adsystem.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final SaleHistoryRepository saleHistoryRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;

    @Autowired
    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             SaleHistoryRepository saleHistoryRepository,
                             UserRepository userRepository,
                             ReviewMapper reviewMapper) {
        this.reviewRepository = reviewRepository;
        this.saleHistoryRepository = saleHistoryRepository;
        this.userRepository = userRepository;
        this.reviewMapper = reviewMapper;
    }

    @Override
    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = "sellerRatings", key = "#result.sellerUsername"),
                    @CacheEvict(value = "userRatings", key = "#result.username"),
                    @CacheEvict(value = "userDetails", key = "#result.sellerUsername")
            }
    )
    public ReviewResponseDto createRating(UUID saleId, ReviewCreateDto reviewCreateDto) {
        log.info("Creating rating for sale: {}", saleId);
        SaleHistory saleHistory = saleHistoryRepository.findById(saleId)
                .orElseThrow(() -> {
                    log.error("Sale history not found for ID: {}", saleId);
                    return new ResourceNotFoundException("SaleHistory", "id", saleId.toString());
                });

        String authenticatedUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User buyer = userRepository.findUserByUsername(authenticatedUsername)
                .orElseThrow(() -> {
                    log.error("Authenticated user not found: {}", authenticatedUsername);
                    return new ResourceNotFoundException("User", "username", authenticatedUsername);
                });

        if (!buyer.getUsername().equals(saleHistory.getBuyer().getUsername())) {
            log.warn("Access denied for user {} to rate sale ID {}", authenticatedUsername, saleId);
            throw new AccessDeniedException("You can only rate purchases you have made");
        }

        boolean isRated = reviewRepository.findRatingBySaleId(saleId).stream()
                .anyMatch(rating -> rating.getUser().getUsername().equals(buyer.getUsername()));
        if (isRated) {
            log.warn("User {} has already rated sale ID {}", authenticatedUsername, saleId);
            throw new AccessDeniedException("You have already rated this sale");
        }

        Review review = new Review();
        review.setUser(buyer);
        review.setSeller(saleHistory.getSeller());
        review.setSale(saleHistory);
        review.setRatingValue(reviewCreateDto.getRatingValue());
        review.setReviewText(reviewCreateDto.getReviewText());

        Review savedReview = reviewRepository.saveAndFlush(review);
        log.info("Rating created. ID: {}, Sale ID: {}", savedReview.getId(), saleId);

        User seller = saleHistory.getSeller();
        List<Review> reviews = reviewRepository.findRatingBySellerUsername(seller.getUsername());
        log.info("Found {} ratings for seller: {}", reviews.size(), seller.getUsername());

        double averageRating = reviews.stream()
                .mapToDouble(Review::getRatingValue)
                .average()
                .orElse(0.0);
        log.info("Average rating for seller {}: {}", seller.getUsername(), averageRating);
        int reviewCount = reviews.size();
        log.info("Review count for seller {}: {}", seller.getUsername(), reviewCount);
        double logCount = Math.log10(reviewCount + 1);
        log.info("Log count for seller {}: {}", seller.getUsername(), logCount);

        seller.setRating((float) ((averageRating * 0.9) + (logCount * 0.3)));
        userRepository.save(seller);
        log.info("Seller rating updated. Username: {}, Rating: {}", seller.getUsername(), seller.getRating());

        return reviewMapper.toDto(savedReview);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "saleRatings", key = "#saleId")
    public List<ReviewResponseDto> getRatingsBySaleId(UUID saleId) {
        log.info("Fetching ratings for sale: {}", saleId);
        List<ReviewResponseDto> result = reviewRepository.findRatingBySaleId(saleId).stream()
                .map(reviewMapper::toDto)
                .collect(Collectors.toList());
        log.info("Found {} ratings for sale: {}", result.size(), saleId);
        return result;
    }
}
