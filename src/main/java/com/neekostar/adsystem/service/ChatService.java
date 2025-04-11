package com.neekostar.adsystem.service;

import com.neekostar.adsystem.dto.ChatResponseDto;
import com.neekostar.adsystem.dto.UnreadChatsInfoDto;
import com.neekostar.adsystem.model.Chat;
import com.neekostar.adsystem.model.User;

import java.util.List;
import java.util.UUID;

public interface ChatService {
    Chat getOrCreateChat(String initiatorUsername, String otherUsername);

    List<ChatResponseDto> getAllChatsForUser(String username);

    UnreadChatsInfoDto getUnreadInfo(String username);

    ChatResponseDto getChatDetails(String username, UUID chatId);

    int getUnreadChatsCount(String username);
}
