package com.neekostar.adsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
@Schema(name = "UserResponseDto", description = "User response data transfer object")
public class UserResponseDto implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "Username", example = "user")
    private String username;

    @Schema(description = "User email", example = "user@mail.com")
    private String email;

    @Schema(description = "User first name", example = "John")
    private String firstName;

    @Schema(description = "User last name", example = "Doe")
    private String lastName;

    @Schema(description = "User avatar URL", example = "https://example.com/avatar.jpg")
    private String avatarUrl;

    @Schema(description = "User rating", example = "4.5")
    private Float rating;

    @Schema(description = "User role", example = "USER")
    private String role;
}
