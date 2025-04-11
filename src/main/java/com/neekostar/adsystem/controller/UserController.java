package com.neekostar.adsystem.controller;

import com.neekostar.adsystem.dto.ErrorResponse;
import com.neekostar.adsystem.dto.ImageUploadDto;
import com.neekostar.adsystem.dto.UserResponseDto;
import com.neekostar.adsystem.dto.UserUpdateDto;
import com.neekostar.adsystem.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(
        name = "User Management",
        description = "This controller handles all operations related to user management. <br><br>" +
                "The available operations include: <ul>" +
                "<li><b>Get All Users</b> – Retrieves a list of all registered users.</li>" +
                "<li><b>Get User by Username</b> – Retrieves detailed information for a specific user based on their username.</li>" +
                "<li><b>Update User</b> – Updates a user's profile data (e.g., first name, last name, email, avatar URL). " +
                "Only the authenticated user can update their own profile.</li>" +
                "<li><b>Delete User</b> – Deletes a user profile. Only the user themselves can perform this operation.</li>" +
                "<li><b>Upload User Avatar</b> – Uploads a new avatar image for the user. If an existing avatar is present, it will be removed.</li>" +
                "<li><b>Remove User Avatar</b> – Removes the current avatar from the user's profile.</li>" +
                "</ul>" +
                "Possible errors include: <br><br>" +
                "<b>ResourceNotFoundException</b> – when a user is not found; <br>" +
                "<b>AccessDeniedException</b> – when the authenticated user attempts to update, delete, or modify another user's profile; <br>" +
                "<b>IllegalArgumentException</b> – if an email is already in use or other invalid data is provided."
)
public class UserController {
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(
            summary = "Get all users",
            description = "Retrieves a list of all registered users. " +
                    "The response is a list of user details. " +
                    "If no users are found, an empty list is returned.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Users retrieved successfully",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = UserResponseDto.class)
                                    )
                            )
                    )
            }
    )
    public ResponseEntity<?> getAllUsers(@RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserResponseDto> userResponseDtos = userService.getAllUsers(pageable);
        return ResponseEntity.status(HttpStatus.OK).body(userResponseDtos);
    }

    @GetMapping("/{username}")
    @Operation(
            summary = "Get user by username",
            description = "Retrieves detailed information for the user specified by the username. " +
                    "If the user is not found, a 404 error is returned.",
            parameters = {
                    @io.swagger.v3.oas.annotations.Parameter(
                            name = "username",
                            description = "The username of the user to retrieve",
                            required = true
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "User retrieved successfully",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = UserResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid username provided",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "BadRequestExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 400,
                                                              "error": "Bad Request",
                                                              "message": "Invalid username",
                                                              "path": "/api/users/john_doe",
                                                              "method": "GET"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        UserResponseDto userResponseDto = userService.getUserByUsername(username);
        return ResponseEntity.status(HttpStatus.OK).body(userResponseDto);
    }

    @PutMapping("/{username}")
    @Operation(
            summary = "Update user",
            description = "Updates the profile of the specified user. Only the authenticated user can update their own profile. " +
                    "The update may include changes to first name, last name, email, and avatar URL. " +
                    "Possible errors include: <ul>" +
                    "<li><b>ResourceNotFoundException</b> – if the user is not found.</li>" +
                    "<li><b>AccessDeniedException</b> – if the authenticated user attempts to update another user's profile.</li>" +
                    "<li><b>IllegalArgumentException</b> – if the new email is already in use.</li>" +
                    "</ul>",
            parameters = {
                    @Parameter(
                            name = "username",
                            description = "The username of the user to update",
                            required = true
                    )
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User update data (firstName, lastName, email, avatarUrl)",
                    required = true,
                    content = @Content(schema = @Schema(implementation = UserUpdateDto.class))
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "User updated successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = UserResponseDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid user data provided or email already in use",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "BadRequestExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 400,
                                                              "error": "Bad Request",
                                                              "message": "User with email john@example.com already exists",
                                                              "path": "/api/users/john_doe",
                                                              "method": "PUT"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Access denied. You can only update your own profile",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "ForbiddenExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 403,
                                                              "error": "Forbidden",
                                                              "message": "You can only update your own profile",
                                                              "path": "/api/users/john_doe",
                                                              "method": "PUT"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    public ResponseEntity<?> updateUser(@PathVariable String username,
                                        @Validated @RequestBody UserUpdateDto userUpdateDto) {
        UserResponseDto userResponseDto = userService.updateUser(username, userUpdateDto);
        return ResponseEntity.status(HttpStatus.OK).body(userResponseDto);
    }

    @DeleteMapping("/{username}")
    @Operation(
            summary = "Delete user",
            description = "Deletes the user profile specified by the username. Only the authenticated user can delete their own profile. " +
                    "Possible errors include: <ul>" +
                    "<li><b>ResourceNotFoundException</b> – if the user is not found.</li>" +
                    "<li><b>AccessDeniedException</b> – if a user attempts to delete another user's profile.</li>" +
                    "</ul>",
            parameters = {
                    @Parameter(
                            name = "username",
                            description = "The username of the user to delete",
                            required = true
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "User deleted successfully"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid username provided",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "BadRequestExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 400,
                                                              "error": "Bad Request",
                                                              "message": "Invalid username",
                                                              "path": "/api/users/john_doe",
                                                              "method": "DELETE"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Access denied. You can only delete your own profile",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "ForbiddenExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 403,
                                                              "error": "Forbidden",
                                                              "message": "You can only delete your own profile",
                                                              "path": "/api/users/john_doe",
                                                              "method": "DELETE"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    public ResponseEntity<?> deleteUser(@PathVariable String username) {
        userService.deleteUser(username);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping(value = "/{username}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload user avatar",
            description = "Uploads a new avatar for the specified user. If an existing avatar is present, it is removed before uploading the new one. " +
                    "Only the authenticated user can upload an avatar for their own profile. " +
                    "Possible errors include: <ul>" +
                    "<li><b>ResourceNotFoundException</b> – if the user is not found.</li>" +
                    "<li><b>AccessDeniedException</b> – if a user attempts to upload an avatar for another user.</li>" +
                    "<li><b>IOException</b> – if there is an error processing the image file.</li>" +
                    "</ul>",
            parameters = {
                    @Parameter(
                            name = "username",
                            description = "The username of the user for whom the avatar is being uploaded",
                            required = true
                    )
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Multipart form-data containing the avatar image file",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ImageUploadDto.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "User avatar uploaded successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = UserResponseDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid image file provided",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "BadRequestExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 400,
                                                              "error": "Bad Request",
                                                              "message": "Invalid image format",
                                                              "path": "/api/users/john_doe/avatar",
                                                              "method": "POST"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Access denied. You can only upload an avatar for your own profile",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "ForbiddenExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 403,
                                                              "error": "Forbidden",
                                                              "message": "You can only upload avatar for your own profile",
                                                              "path": "/api/users/john_doe/avatar",
                                                              "method": "POST"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    public ResponseEntity<?> uploadUserAvatar(@PathVariable String username,
                                              @Validated @ModelAttribute @NonNull ImageUploadDto imageUploadDto) {
        UserResponseDto userResponseDto = userService.uploadUserAvatar(username, imageUploadDto.getFile());
        return ResponseEntity.status(HttpStatus.OK).body(userResponseDto);
    }

    @DeleteMapping("/{username}/avatar")
    @Operation(
            summary = "Remove user avatar",
            description = "Removes the avatar of the specified user. Only the authenticated user can remove their own avatar. " +
                    "Possible errors include: <ul>" +
                    "<li><b>ResourceNotFoundException</b> – if the user is not found.</li>" +
                    "<li><b>AccessDeniedException</b> – if a user attempts to remove an avatar for another user.</li>" +
                    "</ul>",
            parameters = {
                    @Parameter(
                            name = "username",
                            description = "The username of the user whose avatar should be removed",
                            required = true
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "User avatar removed successfully"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid username provided",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "BadRequestExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 400,
                                                              "error": "Bad Request",
                                                              "message": "Invalid username",
                                                              "path": "/api/users/john_doe/avatar",
                                                              "method": "DELETE"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Access denied. You can only remove your own avatar",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "ForbiddenExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 403,
                                                              "error": "Forbidden",
                                                              "message": "You can only remove your own avatar",
                                                              "path": "/api/users/john_doe/avatar",
                                                              "method": "DELETE"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    public ResponseEntity<?> removeUserAvatar(@PathVariable String username) {
        userService.removeUserAvatar(username);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
