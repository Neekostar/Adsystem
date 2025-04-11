package com.neekostar.adsystem.mapper;

import com.neekostar.adsystem.dto.SaleHistoryResponseDto;
import com.neekostar.adsystem.model.SaleHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class, AdMapper.class})
public interface SaleHistoryMapper {
    @Mapping(target = "sellerUsername", source = "saleHistory.seller.username")
    @Mapping(target = "buyerUsername", source = "saleHistory.buyer.username")
    @Mapping(target = "saleId", source = "saleHistory.id")
    @Mapping(target = "adTitle", source = "saleHistory.ad.title")
    SaleHistoryResponseDto toDto(SaleHistory saleHistory);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "seller", ignore = true)
    @Mapping(target = "buyer", ignore = true)
    @Mapping(target = "ad", ignore = true)
    SaleHistory toEntity(SaleHistoryResponseDto saleHistoryResponseDto);
}
