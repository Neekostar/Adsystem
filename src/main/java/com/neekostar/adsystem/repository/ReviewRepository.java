package com.neekostar.adsystem.repository;

import java.util.List;
import java.util.UUID;
import com.neekostar.adsystem.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findRatingBySellerUsername(String sellerUsername);

    List<Review> findRatingBySaleId(UUID saleId);

}
