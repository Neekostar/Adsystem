package com.neekostar.adsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "MessageUpdateDto", description = "DTO for updating a message")
public class MessageUpdateDto {
    @NotBlank(message = "{message.text.notblank}")
    @Size(max = 1000, message = "{message.text.size}")
    @Schema(description = "New message text", example = "Hello!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newMessageText;
}
