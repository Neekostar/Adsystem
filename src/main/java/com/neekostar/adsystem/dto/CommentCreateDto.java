package com.neekostar.adsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(name = "CommentCreateDto", description = "DTO for creating a comment")
public class CommentCreateDto {
    @NotBlank(message = "{comment.text.notblank}")
    @Schema(description = "Comment text", example = "Nice post!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String commentText;
}
