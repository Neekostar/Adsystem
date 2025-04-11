package com.neekostar.adsystem.repository;

import java.util.UUID;
import com.neekostar.adsystem.model.SaleHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SaleHistoryRepository extends JpaRepository<SaleHistory, UUID> {
    Page<SaleHistory> findBySellerUsername(String username, Pageable pageable);

    Page<SaleHistory> findByBuyerUsername(String username, Pageable pageable);
}
