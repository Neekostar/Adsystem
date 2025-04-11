package com.neekostar.adsystem.service;

import com.neekostar.adsystem.dto.SaleHistoryResponseDto;
import com.neekostar.adsystem.model.SaleHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SaleHistoryService {
    Page<SaleHistoryResponseDto> getSalesBySeller(String sellerUsername, Pageable pageable);

    Page<SaleHistoryResponseDto> getPurchasesByBuyer(String buyerUsername, Pageable pageable);
}
