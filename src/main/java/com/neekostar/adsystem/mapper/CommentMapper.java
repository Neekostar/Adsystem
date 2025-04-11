package com.neekostar.adsystem.mapper;

import com.neekostar.adsystem.dto.CommentCreateDto;
import com.neekostar.adsystem.dto.CommentResponseDto;
import com.neekostar.adsystem.model.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface CommentMapper {
    @Mapping(target = "username", source = "comment.user.username")
    @Mapping(target = "createdAt", source = "comment.createdAt")
    CommentResponseDto toDTO(Comment comment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "ad", ignore = true)
    Comment toEntity(CommentCreateDto dto);
}
