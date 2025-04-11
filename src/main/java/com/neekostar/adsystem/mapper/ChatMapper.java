package com.neekostar.adsystem.mapper;

import com.neekostar.adsystem.dto.ChatResponseDto;
import com.neekostar.adsystem.model.Chat;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class, MessageMapper.class})
public interface ChatMapper {

    @Mapping(target = "chatId", source = "chat.id")
    @Mapping(target = "username1", source = "chat.user1.username")
    @Mapping(target = "username2", source = "chat.user2.username")
    @Mapping(target = "messages", source = "chat.messages")
    @Mapping(target = "unreadMessagesCount", source = "chat.unreadMessagesCount")
    @Mapping(target = "createdAt", source = "chat.createdAt")
    @Mapping(target = "updatedAt", source = "chat.updatedAt")
    ChatResponseDto toDto(Chat chat);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user1", ignore = true)
    @Mapping(target = "user2", ignore = true)
    @Mapping(target = "messages", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Chat toEntity(ChatResponseDto dto);
}
