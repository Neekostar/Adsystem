package com.neekostar.adsystem.mapper;

import com.neekostar.adsystem.dto.ReviewResponseDto;
import com.neekostar.adsystem.model.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class, SaleHistoryMapper.class})
public interface ReviewMapper {
    @Mapping(target = "username", source = "review.user.username")
    @Mapping(target = "sellerUsername", source = "review.seller.username")
    @Mapping(target = "saleId", source = "review.sale.id")
    ReviewResponseDto toDto(Review review);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "seller", ignore = true)
    @Mapping(target = "sale", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Review toEntity(ReviewResponseDto reviewResponseDto);
}
