package com.neekostar.adsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(name = "CommentResponseDto", description = "DTO for comment response")
public class CommentResponseDto implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "Comment text", example = "Nice car!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String commentText;

    @Schema(description = "Username of the comment author", example = "john_doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(description = "Date of the comment creation", example = "2021-07-01T12:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createdAt;
}
