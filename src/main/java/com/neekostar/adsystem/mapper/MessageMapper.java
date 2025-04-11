package com.neekostar.adsystem.mapper;

import com.neekostar.adsystem.dto.MessageCreateDto;
import com.neekostar.adsystem.dto.MessageResponseDto;
import com.neekostar.adsystem.model.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface MessageMapper {
    @Mapping(target = "chatId", source = "chat.id")
    @Mapping(target = "messageId", source = "id")
    @Mapping(target = "senderUsername", source = "sender.username")
    @Mapping(target = "recipientUsername", source = "recipient.username")
    @Mapping(target = "content", source = "plainContent")
    @Mapping(target = "read", source = "read")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    MessageResponseDto toDto(Message message);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "chat", ignore = true)
    @Mapping(target = "sender", ignore = true)
    @Mapping(target = "recipient", ignore = true)
    @Mapping(target = "encryptedContent", ignore = true)
    @Mapping(target = "read", ignore = true)
    @Mapping(target = "plainContent", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Message toEntity(MessageCreateDto dto);

}
