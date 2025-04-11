package com.neekostar.adsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(name = "UserLoginDto", description = "User login data transfer object")
public class UserLoginDto {
    @NotBlank(message = "{login.notblank}")
    @Schema(description = "User login", example = "user", requiredMode = Schema.RequiredMode.REQUIRED)
    private String login;

    @NotBlank(message = "{password.notblank}")
    @Schema(description = "User password", example = "password", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
