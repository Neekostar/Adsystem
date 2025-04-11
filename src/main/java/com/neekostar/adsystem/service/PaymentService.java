package com.neekostar.adsystem.service;

import com.neekostar.adsystem.dto.PaymentResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PaymentService {
    PaymentResponseDto createPayment(UUID adId);

    Page<PaymentResponseDto> getPaymentHistory(String username, Pageable pageable);
}
