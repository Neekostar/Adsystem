package com.neekostar.adsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "UserUpdateDto", description = "User update data transfer object")
public class UserUpdateDto {
    @Size(min = 3, max = 50, message = "{first_name.size}")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "{first_name.pattern}")
    @Schema(description = "User first name", example = "John", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;

    @Size(min = 3, max = 50, message = "{last_name.size}")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "{last_name.pattern}")
    @Schema(description = "User last name", example = "Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String lastName;

    @Email(message = "{email.invalid}")
    @Schema(description = "User email", example = "user@mail.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Size(max = 255, message = "{avatar_url.size}")
    @Schema(description = "User avatar URL", example = "https://example.com/avatar.jpg")
    private String avatarUrl;
}
