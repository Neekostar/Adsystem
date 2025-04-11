package com.neekostar.adsystem.service;

import com.neekostar.adsystem.dto.MessageCreateDto;
import com.neekostar.adsystem.dto.MessageResponseDto;
import com.neekostar.adsystem.dto.MessageUpdateDto;

import java.util.List;
import java.util.UUID;

public interface MessageService {
    MessageResponseDto sendMessage(UUID chatId, String senderUsername, MessageCreateDto dto);

    List<MessageResponseDto> getMessagesForChat(String username, UUID chatId);

    void markMessageAsRead(String username, UUID messageId);

    void markAllMessagesAsRead(String username, UUID chatId);

    MessageResponseDto updateMessage(String username, UUID messageId, MessageUpdateDto updateDto);

    void deleteMessage(String username, UUID messageId);
}
