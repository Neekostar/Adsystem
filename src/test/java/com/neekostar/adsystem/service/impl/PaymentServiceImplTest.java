package com.neekostar.adsystem.service.impl;

import com.neekostar.adsystem.dto.PaymentResponseDto;
import com.neekostar.adsystem.exception.AccessDeniedException;
import com.neekostar.adsystem.exception.ResourceNotFoundException;
import com.neekostar.adsystem.mapper.PaymentMapper;
import com.neekostar.adsystem.model.*;
import com.neekostar.adsystem.repository.AdRepository;
import com.neekostar.adsystem.repository.PaymentRepository;
import com.neekostar.adsystem.repository.SaleHistoryRepository;
import com.neekostar.adsystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private AdRepository adRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SaleHistoryRepository saleHistoryRepository;
    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private Authentication authentication;
    @InjectMocks
    private PaymentServiceImpl paymentService;

    private User buyer;
    private User seller;
    private Ad activeAd;
    private Ad soldAd;
    private Payment payment;
    private PaymentResponseDto responseDto;
    private final UUID AD_ID = UUID.randomUUID();
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

        activeAd = new Ad();
        activeAd.setId(AD_ID);
        activeAd.setPrice(BigDecimal.valueOf(100));
        activeAd.setStatus(AdStatus.ACTIVE);
        activeAd.setUser(seller);

        soldAd = new Ad();
        soldAd.setId(AD_ID);
        soldAd.setStatus(AdStatus.SOLD);

        payment = new Payment();
        payment.setUser(buyer);
        payment.setAd(activeAd);
        payment.setAmount(activeAd.getPrice());
        responseDto = new PaymentResponseDto();
        responseDto.setAmount(activeAd.getPrice());
        responseDto.setAdId(AD_ID);
        responseDto.setPaymentDate(LocalDateTime.now());

        when(authentication.getName()).thenReturn(BUYER_USERNAME);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void createPayment_Success() {
        when(userRepository.findUserByUsername(BUYER_USERNAME)).thenReturn(Optional.of(buyer));
        when(adRepository.findById(AD_ID)).thenReturn(Optional.of(activeAd));
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenReturn(payment);
        when(paymentMapper.toDto(payment)).thenReturn(responseDto);

        PaymentResponseDto result = paymentService.createPayment(AD_ID);

        verify(paymentRepository).saveAndFlush(any(Payment.class));
        verify(adRepository).save(activeAd);
        verify(saleHistoryRepository).save(any(SaleHistory.class));
        assertEquals(AdStatus.SOLD, activeAd.getStatus());
        assertEquals(responseDto, result);
    }

    @Test
    void createPayment_BuyerNotFound() {
        when(authentication.getName()).thenReturn("unknown");
        when(userRepository.findUserByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.createPayment(AD_ID));
    }

    @Test
    void createPayment_AdNotFound() {
        when(userRepository.findUserByUsername(BUYER_USERNAME)).thenReturn(Optional.of(buyer));
        when(adRepository.findById(AD_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.createPayment(AD_ID));
    }

    @Test
    void createPayment_AdAlreadySold() {
        when(userRepository.findUserByUsername(BUYER_USERNAME)).thenReturn(Optional.of(buyer));
        when(adRepository.findById(AD_ID)).thenReturn(Optional.of(soldAd));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> paymentService.createPayment(AD_ID));
        assertEquals("Ad is already sold", ex.getMessage());
    }

    @Test
    void createPayment_SelfPurchaseAttempt() {
        User sameUser = new User();
        sameUser.setUsername(SELLER_USERNAME);
        activeAd.setUser(sameUser);

        when(authentication.getName()).thenReturn(SELLER_USERNAME);
        when(userRepository.findUserByUsername(SELLER_USERNAME)).thenReturn(Optional.of(sameUser));
        when(adRepository.findById(AD_ID)).thenReturn(Optional.of(activeAd));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> paymentService.createPayment(AD_ID));
        assertEquals("You can't purchase your own ads", ex.getMessage());
    }

    @Test
    void createPayment_VerifySaleHistoryCreation() {
        when(userRepository.findUserByUsername(BUYER_USERNAME)).thenReturn(Optional.of(buyer));
        when(adRepository.findById(AD_ID)).thenReturn(Optional.of(activeAd));
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenReturn(payment);

        paymentService.createPayment(AD_ID);

        ArgumentCaptor<SaleHistory> captor = ArgumentCaptor.forClass(SaleHistory.class);
        verify(saleHistoryRepository).save(captor.capture());
        SaleHistory history = captor.getValue();
        assertEquals(seller, history.getSeller());
        assertEquals(buyer, history.getBuyer());
        assertEquals(activeAd, history.getAd());
    }

    @Test
    void createPayment_VerifyPaymentAmount() {
        activeAd.setPrice(BigDecimal.valueOf(150));
        when(userRepository.findUserByUsername(BUYER_USERNAME)).thenReturn(Optional.of(buyer));
        when(adRepository.findById(AD_ID)).thenReturn(Optional.of(activeAd));
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenReturn(payment);

        paymentService.createPayment(AD_ID);

        verify(paymentRepository).saveAndFlush(argThat(p ->
                p.getAmount().equals(BigDecimal.valueOf(150)) &&
                        p.getAd().equals(activeAd) &&
                        p.getUser().equals(buyer)
        ));
    }

    @Test
    void getPaymentHistory_Success() {
        Payment payment1 = new Payment();
        Payment payment2 = new Payment();
        List<Payment> paymentList = List.of(payment1, payment2);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Payment> page = new PageImpl<>(paymentList, pageable, paymentList.size());

        when(paymentRepository.findPaymentByUserUsername(BUYER_USERNAME, pageable)).thenReturn(page);
        PaymentResponseDto dto1 = new PaymentResponseDto();
        PaymentResponseDto dto2 = new PaymentResponseDto();
        when(paymentMapper.toDto(payment1)).thenReturn(dto1);
        when(paymentMapper.toDto(payment2)).thenReturn(dto2);

        Page<PaymentResponseDto> result = paymentService.getPaymentHistory(BUYER_USERNAME, pageable);

        assertEquals(2, result.getTotalElements());
        assertEquals(List.of(dto1, dto2), result.getContent());
        verify(paymentRepository).findPaymentByUserUsername(BUYER_USERNAME, pageable);
    }

    @Test
    void getPaymentHistory_EmptyResult() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Payment> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(paymentRepository.findPaymentByUserUsername(BUYER_USERNAME, pageable)).thenReturn(emptyPage);

        Page<PaymentResponseDto> result = paymentService.getPaymentHistory(BUYER_USERNAME, pageable);
        assertTrue(result.getContent().isEmpty());
    }
}
