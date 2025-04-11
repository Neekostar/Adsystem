package com.neekostar.adsystem.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.neekostar.adsystem.dto.ErrorResponse;
import com.neekostar.adsystem.dto.UserChangePasswordDto;
import com.neekostar.adsystem.dto.UserLoginDto;
import com.neekostar.adsystem.dto.UserRegistrationDto;
import com.neekostar.adsystem.dto.UserResponseDto;
import com.neekostar.adsystem.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(
        name = "Auth Management",
        description = "This controller handles all operations related to user authentication. <br><br>" +
                "The available operations include: <ul>" +
                "<li><b>Register</b> – Create a new user account. This operation verifies that the provided " +
                "username and email are unique. If the request is invalid or if the username/email already exists, " +
                "a 400 error is returned.</li>" +
                "<li><b>Login</b> – Authenticate a user using the provided credentials. On success, the authenticated " +
                "user details are returned. If the credentials are incorrect, a 401 error is returned.</li>" +
                "<li><b>Logout</b> – Logs out the currently authenticated user by clearing the security context, " +
                "thereby terminating the session.</li>" +
                "<li><b>Change Password</b> – Allows the currently logged-in user to change their password. " +
                "The operation ensures that the new password is different from the old one and that the provided old " +
                "password matches the current password. If these conditions are not met, a 400 error is returned.</li>" +
                "</ul>"
)
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Registers a new user with the provided information. " +
                    "On success, returns the created user details.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User registration information",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserRegistrationDto.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "User registered successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = UserResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request body or duplicate username/email",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "DuplicateUsernameExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 400,
                                                              "error": "Bad Request",
                                                              "message": "User with username 'john_doe' already exists",
                                                              "path": "/api/auth/register",
                                                              "method": "POST"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    public ResponseEntity<?> register(@Validated @RequestBody UserRegistrationDto userRegistrationDto) {
        UserResponseDto userResponseDto = authService.register(userRegistrationDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponseDto);
    }

    @PostMapping("/login")
    @Operation(
            summary = "Login",
            description = "Logs in with the provided credentials. On success, returns the authenticated user details.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User login information",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserLoginDto.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Login successful",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = UserResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request body",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "BadRequestExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 400,
                                                              "error": "Bad Request",
                                                              "message": "Missing required field: password",
                                                              "path": "/api/auth/login",
                                                              "method": "POST"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Invalid credentials",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "UnauthorizedExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 401,
                                                              "error": "Unauthorized",
                                                              "message": "Invalid login credentials",
                                                              "path": "/api/auth/login",
                                                              "method": "POST"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    public ResponseEntity<?> login(@Validated @RequestBody UserLoginDto userLoginDto) {
        UserResponseDto userResponseDto = authService.login(userLoginDto);
        return ResponseEntity.status(HttpStatus.OK).body(userResponseDto);
    }


    @PostMapping("/logout")
    @Operation(
            summary = "Logout",
            description = "Logs out the currently logged-in user. " +
                    "Clears the security context and invalidates the session.",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "Logout successful"
                    )
            }
    )
    public ResponseEntity<?> logout(HttpServletRequest request,
                                    HttpServletResponse response) {
        authService.logout();
        new SecurityContextLogoutHandler().logout(request, response, null);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }


    @PostMapping("/change-password")
    @Operation(
            summary = "Change password",
            description = "Changes the password of the currently logged-in user. " +
                    "The old password must match the current one, and the new password must be different and non-blank.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Object containing the old and new password",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserChangePasswordDto.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Password changed successfully"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request body or password validation error",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "BadRequestExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 400,
                                                              "error": "Bad Request",
                                                              "message": "New password must be different from the old one",
                                                              "path": "/api/auth/change-password",
                                                              "method": "POST"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    public ResponseEntity<?> changePassword(@Validated @RequestBody UserChangePasswordDto userChangePasswordDto) {
        authService.changePassword(userChangePasswordDto.getOldPassword(), userChangePasswordDto.getNewPassword());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

}
