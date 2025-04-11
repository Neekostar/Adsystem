package com.neekostar.adsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Schema(name = "MessageResponseDto", description = "DTO for message response")
public class MessageResponseDto {
    @Schema(description = "Chat ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID chatId;

    @Schema(description = "Message ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID messageId;

    @Schema(description = "Sender username", example = "john_doe")
    private String senderUsername;

    @Schema(description = "Recipient username", example = "jane_doe")
    private String recipientUsername;

    @Schema(description = "Decrypted message content")
    private String content;

    @Schema(description = "Is the message read")
    private boolean isRead;

    @Schema(description = "Date of the message creation", example = "2021-07-01T12:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Date of the message update", example = "2021-07-01T12:00:00")
    private LocalDateTime updatedAt;
}
