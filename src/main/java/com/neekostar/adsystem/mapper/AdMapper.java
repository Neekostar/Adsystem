package com.neekostar.adsystem.mapper;

import com.neekostar.adsystem.dto.AdCreateDto;
import com.neekostar.adsystem.dto.AdResponseDto;
import com.neekostar.adsystem.dto.AdUpdateDto;
import com.neekostar.adsystem.model.Ad;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class, CommentMapper.class})
public interface AdMapper {
    @Mapping(target = "username", source = "ad.user.username")
    @Mapping(target = "comments", source = "ad.comments")
    @Mapping(target = "status", expression = "java(ad.getStatus().name())")
    @Mapping(target = "userRating", source = "ad.user.rating")
    AdResponseDto toDto(Ad ad);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isPromoted", ignore = true)
    @Mapping(target = "promotionEndDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    Ad toEntity(AdUpdateDto adUpdateDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isPromoted", ignore = true)
    @Mapping(target = "promotionEndDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    Ad toEntity(AdCreateDto adCreateDto);
}
