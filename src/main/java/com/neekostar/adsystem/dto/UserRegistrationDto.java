package com.neekostar.adsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "UserRegistrationDto", description = "User registration data transfer object")
public class UserRegistrationDto {
    @NotBlank(message = "{username.notblank}")
    @Size(min = 3, max = 50, message = "{username.size}")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "{username.pattern}")
    @Schema(description = "User username", example = "user", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "{password.notblank}")
    @Size(min = 6, max = 255, message = "{password.size}")
    @Schema(description = "User password", example = "password", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotBlank(message = "{email.notblank}")
    @Email(message = "{email.invalid}")
    @Schema(description = "User email", example = "user@mail.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Size(min = 3, max = 50, message = "{first_name.size}")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "{first_name.pattern}")
    @Schema(description = "User first name", example = "John")
    private String firstName;

    @Size(min = 3, max = 50, message = "{last_name.size}")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "{last_name.pattern}")
    @Schema(description = "User last name", example = "Doe")
    private String lastName;
}
