package com.neekostar.adsystem.mapper;

import com.neekostar.adsystem.dto.UserRegistrationDto;
import com.neekostar.adsystem.dto.UserResponseDto;
import com.neekostar.adsystem.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "role", source = "user.role.name")
    UserResponseDto toDto(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rating", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    User toEntity(UserRegistrationDto userRegistrationDto);
}
