package com.neekostar.adsystem.service.impl;

import com.neekostar.adsystem.dto.SaleHistoryResponseDto;
import com.neekostar.adsystem.mapper.SaleHistoryMapper;
import com.neekostar.adsystem.repository.SaleHistoryRepository;
import com.neekostar.adsystem.service.SaleHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class SaleHistoryServiceImpl implements SaleHistoryService {

    private final SaleHistoryRepository saleHistoryRepository;
    private final SaleHistoryMapper saleHistoryMapper;

    @Autowired
    public SaleHistoryServiceImpl(SaleHistoryRepository saleHistoryRepository,
                                  SaleHistoryMapper saleHistoryMapper) {
        this.saleHistoryRepository = saleHistoryRepository;
        this.saleHistoryMapper = saleHistoryMapper;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "sellerSales", key = "#sellerUsername + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<SaleHistoryResponseDto> getSalesBySeller(String sellerUsername, Pageable pageable) {
        log.info("Getting sales by seller: {}", sellerUsername);
        if (sellerUsername == null || sellerUsername.isEmpty()) {
            log.warn("Invalid seller username provided: {}", sellerUsername);
            throw new IllegalArgumentException("Invalid seller username");
        }
        Page<SaleHistoryResponseDto> result = saleHistoryRepository
                .findBySellerUsername(sellerUsername, pageable)
                .map(saleHistoryMapper::toDto);
        log.info("Found {} sales by seller: {}", result.getTotalElements(), sellerUsername);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "buyerPurchases", key = "#buyerUsername")
    public Page<SaleHistoryResponseDto> getPurchasesByBuyer(String buyerUsername, Pageable pageable) {
        log.info("Getting purchases by buyer: {}", buyerUsername);
        Page<SaleHistoryResponseDto> result = (Page<SaleHistoryResponseDto>) saleHistoryRepository
                .findByBuyerUsername(buyerUsername, pageable)
                .filter(saleHistory -> saleHistory.getBuyer().getUsername().equals(buyerUsername))
                .map(saleHistoryMapper::toDto);
        log.info("Found {} purchases by buyer: {}", result.getTotalElements(), buyerUsername);
        return result;
    }
}
