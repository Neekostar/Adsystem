package com.neekostar.adsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "UnreadChatsInfoDto", description = "DTO for unread chats info")
public class UnreadChatsInfoDto {

    @Schema(description = "Total unread messages across all chats", example = "5")
    private int totalUnread;

    @Schema(description = "List of unread messages count per chat")
    private List<ChatUnreadDto> chats;
}
