package com.neekostar.adsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(name = "ErrorResponse", description = "DTO for error response")
public class ErrorResponse {
    @Schema(description = "Date and time of the error", example = "2021-07-01T12:00:00")
    private LocalDateTime timestamp;

    @Schema(description = "Error status code", example = "400")
    private int status;

    @Schema(description = "Error type", example = "Bad Request")
    private String error;

    @Schema(description = "Error message", example = "Validation failed")
    private String message;

    @Schema(description = "List of errors",
            example = "[\"title must not be blank\",\"price must be positive\"]")
    private List<String> errors;

    @Schema(description = "Request path", example = "/api/ads")
    private String path;

    @Schema(description = "Request method", example = "POST")
    private String method;
}
