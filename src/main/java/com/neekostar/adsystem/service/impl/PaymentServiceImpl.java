package com.neekostar.adsystem.service.impl;

import java.util.UUID;
import com.neekostar.adsystem.dto.PaymentResponseDto;
import com.neekostar.adsystem.exception.AccessDeniedException;
import com.neekostar.adsystem.exception.ResourceNotFoundException;
import com.neekostar.adsystem.mapper.PaymentMapper;
import com.neekostar.adsystem.model.Ad;
import com.neekostar.adsystem.model.AdStatus;
import com.neekostar.adsystem.model.Payment;
import com.neekostar.adsystem.model.SaleHistory;
import com.neekostar.adsystem.model.User;
import com.neekostar.adsystem.repository.AdRepository;
import com.neekostar.adsystem.repository.PaymentRepository;
import com.neekostar.adsystem.repository.SaleHistoryRepository;
import com.neekostar.adsystem.repository.UserRepository;
import com.neekostar.adsystem.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final AdRepository adRepository;
    private final UserRepository userRepository;
    private final SaleHistoryRepository saleHistoryRepository;
    private final PaymentMapper paymentMapper;

    @Autowired
    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              AdRepository adRepository,
                              UserRepository userRepository,
                              SaleHistoryRepository saleHistoryRepository,
                              PaymentMapper paymentMapper) {
        this.paymentRepository = paymentRepository;
        this.adRepository = adRepository;
        this.userRepository = userRepository;
        this.saleHistoryRepository = saleHistoryRepository;
        this.paymentMapper = paymentMapper;
    }

    @Override
    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = "paymentHistory", allEntries = true),
                    @CacheEvict(value = "sellerSales", allEntries = true),
                    @CacheEvict(value = "buyerPurchases", allEntries = true),
                    @CacheEvict(value = "singleAd", key = "#adId"),
                    @CacheEvict(value = "userAds", allEntries = true),
                    @CacheEvict(value = {"promotedAds", "nonPromotedAds", "filteredAds"}, allEntries = true)
            }
    )
    public PaymentResponseDto createPayment(UUID adId) {
        log.info("Starting payment process for ad: {}", adId);
        String authenticatedUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User buyer = userRepository.findUserByUsername(authenticatedUsername)
                .orElseThrow(() -> {
                    log.error("Buyer not found: {}", authenticatedUsername);
                    return new ResourceNotFoundException("User", "username", authenticatedUsername);
                });

        Ad ad = adRepository.findById(adId)
                .orElseThrow(() -> {
                    log.error("Ad not found: {}", adId);
                    return new ResourceNotFoundException("Ad", "id", adId.toString());
                });

        if (ad.getStatus() == AdStatus.SOLD) {
            log.warn("Attempt to purchase sold ad: {}", adId);
            throw new AccessDeniedException("Ad is already sold");
        }

        if (ad.getUser().getUsername().equals(authenticatedUsername)) {
            log.warn("User {} tried to purchase own ad: {}", authenticatedUsername, adId);
            throw new AccessDeniedException("You can't purchase your own ads");
        }

        Payment payment = new Payment();
        payment.setUser(buyer);
        payment.setAd(ad);
        payment.setAmount(ad.getPrice());
        Payment savedPayment = paymentRepository.saveAndFlush(payment);
        log.info("Payment created. ID: {}, Amount: {}", savedPayment.getId(), savedPayment.getAmount());

        ad.setStatus(AdStatus.SOLD);
        adRepository.save(ad);
        log.info("Ad status updated to SOLD: {}", adId);

        SaleHistory saleHistory = new SaleHistory();
        saleHistory.setSeller(ad.getUser());
        saleHistory.setBuyer(buyer);
        saleHistory.setAd(ad);
        saleHistoryRepository.save(saleHistory);
        log.info("Sale history recorded. Ad: {}, Buyer: {}", adId, authenticatedUsername);

        return paymentMapper.toDto(savedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "paymentHistory", key = "#username + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<PaymentResponseDto> getPaymentHistory(String username, Pageable pageable) {
        log.info("Fetching payment history for user: {}", username);
        Page<Payment> payments = paymentRepository.findPaymentByUserUsername(username, pageable);

        log.info("Found {} payments for user: {}", payments.getTotalElements(), username);
        return payments.map(paymentMapper::toDto);
    }
}
