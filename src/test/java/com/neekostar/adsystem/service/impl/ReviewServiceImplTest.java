package com.neekostar.adsystem.service.impl;

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
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ReviewServiceImplTest {
    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private SaleHistoryRepository saleHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ReviewServiceImpl ratingService;

    private User buyer;
    private User seller;
    private SaleHistory saleHistory;
    private ReviewCreateDto reviewCreateDto;
    private final UUID SALE_ID = UUID.randomUUID();
    private final String BUYER_USERNAME = "buyer";
    private final String SELLER_USERNAME = "seller";

    @BeforeEach
    void setUp() {
        buyer = new User();
        buyer.setUsername(BUYER_USERNAME);
        buyer.setEmail("buyer@test.com");

        seller = new User();
        seller.setUsername(SELLER_USERNAME);
        seller.setEmail("seller@test.com");
        seller.setRating(4.0f);

        saleHistory = new SaleHistory();
        saleHistory.setId(SALE_ID);
        saleHistory.setBuyer(buyer);
        saleHistory.setSeller(seller);
        saleHistory.setSaleDate(LocalDateTime.now());

        reviewCreateDto = new ReviewCreateDto();
        reviewCreateDto.setRatingValue(5);
        reviewCreateDto.setReviewText("Great service!");

        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
    }

    @Test
    void createRating_Success() {
        when(authentication.getName()).thenReturn(BUYER_USERNAME);
        when(saleHistoryRepository.findById(SALE_ID)).thenReturn(Optional.of(saleHistory));
        when(userRepository.findUserByUsername(BUYER_USERNAME)).thenReturn(Optional.of(buyer));
        when(reviewRepository.findRatingBySaleId(SALE_ID)).thenReturn(Collections.emptyList());

        List<Review> mockRatingsForSeller = new ArrayList<>();

        when(reviewRepository.saveAndFlush(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0, Review.class);
            mockRatingsForSeller.add(r);
            return r;
        });

        when(reviewRepository.findRatingBySellerUsername(SELLER_USERNAME)).thenReturn(mockRatingsForSeller);

        ratingService.createRating(SALE_ID, reviewCreateDto);

        verify(reviewRepository).saveAndFlush(any(Review.class));
        verify(userRepository).save(seller);

        float expected = 4.59f;
        assertEquals(expected, seller.getRating(), 0.01f);
    }


    @Test
    void createRating_SaleNotFound() {
        when(saleHistoryRepository.findById(SALE_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> ratingService.createRating(SALE_ID, reviewCreateDto));
    }

    @Test
    void createRating_UserNotBuyer() {
        when(authentication.getName()).thenReturn("other_user");
        User notBuyer = new User();
        notBuyer.setUsername("other_user");
        when(userRepository.findUserByUsername("other_user")).thenReturn(Optional.of(notBuyer));

        when(saleHistoryRepository.findById(SALE_ID)).thenReturn(Optional.of(saleHistory));

        assertThrows(AccessDeniedException.class,
                () -> ratingService.createRating(SALE_ID, reviewCreateDto));
    }

    @Test
    void createRating_DuplicateRating() {
        Review existingReview = new Review();
        existingReview.setUser(buyer);

        when(saleHistoryRepository.findById(SALE_ID)).thenReturn(Optional.of(saleHistory));
        when(authentication.getName()).thenReturn(BUYER_USERNAME);
        when(userRepository.findUserByUsername(BUYER_USERNAME)).thenReturn(Optional.of(buyer));
        when(reviewRepository.findRatingBySaleId(SALE_ID)).thenReturn(List.of(existingReview));

        assertThrows(AccessDeniedException.class,
                () -> ratingService.createRating(SALE_ID, reviewCreateDto));
    }

    @Test
    void createRating_VerifyRatingCalculation() {
        User otherBuyer1 = new User();
        otherBuyer1.setUsername("otherBuyer1");
        User otherBuyer2 = new User();
        otherBuyer2.setUsername("otherBuyer2");

        Review r1 = createTestRating(4);
        r1.setUser(otherBuyer1);
        Review r2 = createTestRating(5);
        r2.setUser(otherBuyer2);

        List<Review> existingReviews = Arrays.asList(r1, r2);

        when(authentication.getName()).thenReturn(BUYER_USERNAME);
        when(saleHistoryRepository.findById(SALE_ID)).thenReturn(Optional.of(saleHistory));
        when(userRepository.findUserByUsername(BUYER_USERNAME)).thenReturn(Optional.of(buyer));
        when(reviewRepository.findRatingBySaleId(SALE_ID)).thenReturn(existingReviews);

        List<Review> mockRatingsForSeller = new ArrayList<>(existingReviews);

        when(reviewRepository.saveAndFlush(any(Review.class))).thenAnswer(inv -> {
            Review newR = inv.getArgument(0, Review.class);
            mockRatingsForSeller.add(newR);
            return newR;
        });

        when(reviewRepository.findRatingBySellerUsername(SELLER_USERNAME))
                .thenReturn(mockRatingsForSeller);

        ratingService.createRating(SALE_ID, reviewCreateDto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        double expectedAverage = (4 + 5 + 5) / 3.0;
        double expectedLog = Math.log10(3 + 1);
        double expectedRating = (expectedAverage * 0.9) + (expectedLog * 0.3);

        assertEquals(expectedRating, userCaptor.getValue().getRating(), 0.01);
    }

    @Test
    void createRating_AuthenticatedUserNotFound_ShouldThrowResourceNotFoundException() {
        when(authentication.getName()).thenReturn(BUYER_USERNAME);
        when(saleHistoryRepository.findById(SALE_ID)).thenReturn(Optional.of(saleHistory));
        when(userRepository.findUserByUsername(BUYER_USERNAME)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> ratingService.createRating(SALE_ID, reviewCreateDto));

        assertTrue(ex.getMessage().contains("User"));
    }

    @Test
    void getRatingsBySaleId_Success() {
        Review review = new Review();
        review.setRatingValue(5);
        ReviewResponseDto dto = new ReviewResponseDto();

        when(reviewRepository.findRatingBySaleId(SALE_ID)).thenReturn(List.of(review));
        when(reviewMapper.toDto(review)).thenReturn(dto);

        List<ReviewResponseDto> result = ratingService.getRatingsBySaleId(SALE_ID);

        assertEquals(1, result.size());
        assertSame(dto, result.get(0));
    }

    @Test
    void getRatingsBySaleId_EmptyList() {
        when(reviewRepository.findRatingBySaleId(SALE_ID)).thenReturn(Collections.emptyList());

        List<ReviewResponseDto> result = ratingService.getRatingsBySaleId(SALE_ID);

        assertTrue(result.isEmpty());
    }

    private @NotNull Review createTestRating(int value) {
        Review review = new Review();
        review.setRatingValue(value);
        review.setUser(buyer);
        review.setSeller(seller);
        return review;
    }
}
