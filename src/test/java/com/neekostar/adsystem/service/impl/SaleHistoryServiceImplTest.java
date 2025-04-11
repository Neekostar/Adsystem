package com.neekostar.adsystem.service.impl;

import com.neekostar.adsystem.dto.SaleHistoryResponseDto;
import com.neekostar.adsystem.mapper.SaleHistoryMapper;
import com.neekostar.adsystem.model.SaleHistory;
import com.neekostar.adsystem.model.User;
import com.neekostar.adsystem.repository.SaleHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SaleHistoryServiceImplTest {

    @Mock
    private SaleHistoryRepository saleHistoryRepository;
    @Mock
    private SaleHistoryMapper saleHistoryMapper;
    @InjectMocks
    private SaleHistoryServiceImpl saleHistoryService;

    private SaleHistory saleHistory;
    private SaleHistoryResponseDto saleHistoryResponseDto;
    private User seller;
    private User buyer;
    private final String SELLER_USERNAME = "seller";
    private final String BUYER_USERNAME = "buyer";
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        seller = new User();
        seller.setUsername(SELLER_USERNAME);

        buyer = new User();
        buyer.setUsername(BUYER_USERNAME);

        saleHistory = new SaleHistory();
        saleHistory.setId(UUID.randomUUID());
        saleHistory.setSeller(seller);
        saleHistory.setBuyer(buyer);
        saleHistory.setSaleDate(LocalDateTime.now());

        saleHistoryResponseDto = new SaleHistoryResponseDto();
        saleHistoryResponseDto.setSellerUsername(SELLER_USERNAME);
        saleHistoryResponseDto.setBuyerUsername(BUYER_USERNAME);
        saleHistoryResponseDto.setSaleDate(saleHistory.getSaleDate());

        pageable = PageRequest.of(0, 10);
    }

    static class CustomPageImpl<T> extends PageImpl<T> {
        public CustomPageImpl(List<T> content, Pageable pageable, long total) {
            super(content, pageable, total);
        }
        @Override
        public CustomPageImpl<T> filter(java.util.function.Predicate<? super T> predicate) {
            List<T> filtered = getContent().stream().filter(predicate).toList();
            return new CustomPageImpl<>(filtered, getPageable(), filtered.size());
        }
    }

    @Test
    void getSalesBySeller_Success() {
        Page<SaleHistory> page = new PageImpl<>(List.of(saleHistory), pageable, 1);
        when(saleHistoryRepository.findBySellerUsername(SELLER_USERNAME, pageable)).thenReturn(page);
        when(saleHistoryMapper.toDto(saleHistory)).thenReturn(saleHistoryResponseDto);

        Page<SaleHistoryResponseDto> result = saleHistoryService.getSalesBySeller(SELLER_USERNAME, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(saleHistoryResponseDto, result.getContent().get(0));
        verify(saleHistoryRepository).findBySellerUsername(SELLER_USERNAME, pageable);
        verify(saleHistoryMapper).toDto(saleHistory);
    }

    @Test
    void getSalesBySeller_EmptyList() {
        Page<SaleHistory> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        when(saleHistoryRepository.findBySellerUsername(SELLER_USERNAME, pageable)).thenReturn(emptyPage);

        Page<SaleHistoryResponseDto> result = saleHistoryService.getSalesBySeller(SELLER_USERNAME, pageable);
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(saleHistoryRepository).findBySellerUsername(SELLER_USERNAME, pageable);
        verify(saleHistoryMapper, never()).toDto(any());
    }

    @Test
    void getSalesBySeller_NullSellerUsername() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                saleHistoryService.getSalesBySeller(null, pageable)
        );
        assertEquals("Invalid seller username", exception.getMessage());
    }

    @Test
    void getSalesBySeller_EmptySellerUsername() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                saleHistoryService.getSalesBySeller("", pageable)
        );
        assertEquals("Invalid seller username", exception.getMessage());
    }


    @Test
    void getPurchasesByBuyer_Success() {
        Page<SaleHistory> page = new CustomPageImpl<>(List.of(saleHistory), pageable, 1);
        when(saleHistoryRepository.findByBuyerUsername(BUYER_USERNAME, pageable)).thenReturn(page);
        when(saleHistoryMapper.toDto(saleHistory)).thenReturn(saleHistoryResponseDto);

        Page<SaleHistoryResponseDto> result = saleHistoryService.getPurchasesByBuyer(BUYER_USERNAME, pageable);
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(saleHistoryResponseDto, result.getContent().get(0));
        verify(saleHistoryRepository).findByBuyerUsername(BUYER_USERNAME, pageable);
        verify(saleHistoryMapper).toDto(saleHistory);
    }

    @Test
    void getPurchasesByBuyer_EmptyList() {
        Page<SaleHistory> emptyPage = new CustomPageImpl<>(List.of(), pageable, 0);
        when(saleHistoryRepository.findByBuyerUsername(BUYER_USERNAME, pageable)).thenReturn(emptyPage);

        Page<SaleHistoryResponseDto> result = saleHistoryService.getPurchasesByBuyer(BUYER_USERNAME, pageable);
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
        verify(saleHistoryRepository).findByBuyerUsername(BUYER_USERNAME, pageable);
        verify(saleHistoryMapper, never()).toDto(any());
    }

    @Test
    void getPurchasesByBuyer_FilterByBuyerUsername() {
        User anotherBuyer = new User();
        anotherBuyer.setUsername("otherBuyer");

        SaleHistory otherSale = new SaleHistory();
        otherSale.setId(UUID.randomUUID());
        otherSale.setSeller(seller);
        otherSale.setBuyer(anotherBuyer);
        otherSale.setSaleDate(LocalDateTime.now());

        Page<SaleHistory> page = new CustomPageImpl<>(List.of(saleHistory, otherSale), pageable, 2);
        when(saleHistoryRepository.findByBuyerUsername(BUYER_USERNAME, pageable)).thenReturn(page);
        when(saleHistoryMapper.toDto(saleHistory)).thenReturn(saleHistoryResponseDto);

        Page<SaleHistoryResponseDto> result = saleHistoryService.getPurchasesByBuyer(BUYER_USERNAME, pageable);
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(saleHistoryResponseDto, result.getContent().get(0));
        verify(saleHistoryRepository).findByBuyerUsername(BUYER_USERNAME, pageable);
        verify(saleHistoryMapper).toDto(saleHistory);
        verify(saleHistoryMapper, never()).toDto(otherSale);
    }
}
