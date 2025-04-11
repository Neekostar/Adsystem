package com.neekostar.adsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Schema(name = "ChatResponseDto", description = "DTO for chat response")
public class ChatResponseDto {
    @Schema(description = "Chat ID", example = "123e4567-e89b-12d3-a456-426614174000", required = true)
    private UUID chatId;

    @Schema(description = "First user's username", example = "john_doe", required = true)
    private String username1;

    @Schema(description = "Second user's username", example = "jane_doe", required = true)
    private String username2;

    @Schema(description = "List of messages in the chat", required = true)
    private List<MessageResponseDto> messages;

    @Schema(description = "Date of the chat creation", example = "2021-07-01T12:00:00", required = true)
    private LocalDateTime createdAt;

    @Schema(description = "Date of the last message in the chat", example = "2021-07-01T12:00:00", required = true)
    private LocalDateTime updatedAt;

    @Schema(description = "Number of unread messages in the chat", example = "2", required = true)
    private int unreadMessagesCount;
}
