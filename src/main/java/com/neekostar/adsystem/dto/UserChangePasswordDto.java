package com.neekostar.adsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(name = "UserChangePasswordDto", description = "User change password data transfer object")
public class UserChangePasswordDto {
    @NotBlank(message = "{password.notblank}")
    @Schema(description = "User old password", example = "password", requiredMode = Schema.RequiredMode.REQUIRED)
    private String oldPassword;

    @NotBlank(message = "{password.notblank}")
    @Schema(description = "User new password", example = "password", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;
}
