package com.neekostar.adsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(name = "MessageCreateDto", description = "DTO for creating a message")
public class MessageCreateDto {
    @NotBlank(message = "{message.text.notblank}")
    @Size(max = 1000, message = "{message.text.size}")
    @Schema(description = "Message text", example = "Hello!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String messageText;
}
