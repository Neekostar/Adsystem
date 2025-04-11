package com.neekostar.adsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(name = "ChatUnreadDto", description = "DTO for unread chat count")
public class ChatUnreadDto {

    @Schema(description = "Chat ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID chatId;

    @Schema(description = "Unread message count in this chat", example = "5")
    private int unreadCount;
}
