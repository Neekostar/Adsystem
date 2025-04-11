package com.neekostar.adsystem.controller;

import java.util.List;
import java.util.UUID;
import com.neekostar.adsystem.dto.ChatResponseDto;
import com.neekostar.adsystem.dto.ErrorResponse;
import com.neekostar.adsystem.dto.UnreadChatsInfoDto;
import com.neekostar.adsystem.model.Chat;
import com.neekostar.adsystem.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chats")
@Tag(
        name = "Chat Management",
        description = "This controller handles all operations related to managing chats between users. <br><br>" +
                "The available operations include: <ul>" +
                "<li><b>Get All Chats</b> – Retrieves all chats for a specified user. " +
                "Ensures that the authenticated user is the same as the requested user. Returns detailed chat data with decrypted message content.</li>" +
                "<li><b>Get Unread Chats Count</b> – Returns the number of chats that contain unread messages for the user.</li>" +
                "<li><b>Create/Get Chat</b> – Retrieves an existing chat between two users or creates a new one if it does not exist. " +
                "Validates that a user cannot create a chat with themselves and that the requester is the chat initiator.</li>" +
                "<li><b>Get Chat Details</b> – Retrieves detailed information of a specific chat including all messages with decrypted content. " +
                "Verifies that the chat belongs to the user.</li>" +
                "<li><b>Get Unread Chats Info</b> – Provides aggregated information about unread messages across all chats for the user, " +
                "including details per chat and total unread count.</li>" +
                "</ul>" +
                "Possible errors include: <br><br>" +
                "<b>ResourceNotFoundException</b> – when a chat or user is not found; <br>" +
                "<b>AccessDeniedException</b> – when the authenticated user attempts to access chats that do not belong to them."
)
public class ChatController {

    private final ChatService chatService;

    @Autowired
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/{username}")
    @Operation(
            summary = "Retrieve all chats for a user",
            description = "Fetches all chats associated with the specified username. " +
                    "The authenticated user must match the provided username. " +
                    "On success, returns a list of chat details with decrypted messages. " +
                    "Possible errors: 403 (Access denied) if the user is not authorized, 404 (User not found) if the user does not exist.",
            parameters = {
                    @io.swagger.v3.oas.annotations.Parameter(
                            name = "username",
                            description = "Username for which to retrieve chats",
                            required = true
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Chats retrieved successfully",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ChatResponseDto.class),
                                    array = @io.swagger.v3.oas.annotations.media.ArraySchema(schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ChatResponseDto.class))
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Access denied. The authenticated user is not authorized to access these chats",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "ForbiddenExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 403,
                                                              "error": "Forbidden",
                                                              "message": "You can only access your own chats",
                                                              "path": "/api/chats/johndoe",
                                                              "method": "GET"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "User not found",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "NotFoundExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 404,
                                                              "error": "Not Found",
                                                              "message": "User not found",
                                                              "path": "/api/chats/johndoe",
                                                              "method": "GET"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    public ResponseEntity<?> getAllChatsForCurrentUser(@PathVariable String username) {
        List<ChatResponseDto> chats = chatService.getAllChatsForUser(username);
        return ResponseEntity.status(HttpStatus.OK).body(chats);
    }

    @GetMapping("/{username}/unread-count")
    @Operation(
            summary = "Retrieve unread chats count",
            description = "Returns the number of chats that contain unread messages for the specified user. " +
                    "The authenticated user must match the provided username. " +
                    "Possible error: 403 if access is denied.",
            parameters = {
                    @io.swagger.v3.oas.annotations.Parameter(
                            name = "username",
                            description = "Username for which to count unread chats",
                            required = true
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Unread chats count retrieved successfully",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = Integer.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Access denied. The authenticated user is not authorized to access this information",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "ForbiddenExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 403,
                                                              "error": "Forbidden",
                                                              "message": "You can only access your own chats",
                                                              "path": "/api/chats/johndoe/unread-count",
                                                              "method": "GET"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    public ResponseEntity<?> getUnreadChatsCount(@PathVariable String username) {
        int count = chatService.getUnreadChatsCount(username);
        return ResponseEntity.status(HttpStatus.OK).body(count);
    }

    @PostMapping("/{username}/create/{otherUsername}")
    @Operation(
            summary = "Create or retrieve a chat",
            description = "Retrieves an existing chat between the specified initiator and the other user, " +
                    "or creates a new chat if one does not exist. " +
                    "The initiator (specified by {username}) must match the authenticated user, " +
                    "and a user cannot create a chat with themselves. " +
                    "Possible errors: 400 if the usernames are identical, 403 if the authenticated user does not match, " +
                    "and 404 if either user is not found.",
            parameters = {
                    @io.swagger.v3.oas.annotations.Parameter(
                            name = "username",
                            description = "Username of the chat initiator (must match the authenticated user)",
                            required = true
                    ),
                    @io.swagger.v3.oas.annotations.Parameter(
                            name = "otherUsername",
                            description = "Username of the other participant",
                            required = true
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Chat created successfully; returns the chat ID",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = UUID.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request (e.g., trying to create a chat with yourself)",
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
                                                              "message": "Users cannot create a chat with themselves",
                                                              "path": "/api/chats/johndoe/create/johndoe",
                                                              "method": "POST"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Access denied if the authenticated user does not match the initiator",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "ForbiddenExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 403,
                                                              "error": "Forbidden",
                                                              "message": "You can only create a chat on your own behalf",
                                                              "path": "/api/chats/johndoe/create/janedoe",
                                                              "method": "POST"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "One or both users not found",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "NotFoundExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 404,
                                                              "error": "Not Found",
                                                              "message": "User 'janedoe' not found",
                                                              "path": "/api/chats/johndoe/create/janedoe",
                                                              "method": "POST"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    public ResponseEntity<?> getOrCreateChat(@PathVariable String username,
                                             @PathVariable String otherUsername) {
        Chat chat = chatService.getOrCreateChat(username, otherUsername);
        return ResponseEntity.status(HttpStatus.CREATED).body(chat.getId());
    }

    @GetMapping("/{username}/{chatId}")
    @Operation(
            summary = "Retrieve chat details",
            description = "Fetches detailed information for a specific chat identified by its ID. " +
                    "The authenticated user must be a participant of the chat. " +
                    "Returns all messages with decrypted content. " +
                    "Possible errors: 403 if the user is not authorized or 404 if the chat is not found.",
            parameters = {
                    @io.swagger.v3.oas.annotations.Parameter(
                            name = "username",
                            description = "Username of the requesting user",
                            required = true
                    ),
                    @io.swagger.v3.oas.annotations.Parameter(
                            name = "chatId",
                            description = "Unique identifier (UUID) of the chat",
                            required = true
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Chat details retrieved successfully",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ChatResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Access denied. The chat does not belong to the authenticated user",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "ForbiddenExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 403,
                                                              "error": "Forbidden",
                                                              "message": "Chat does not belong to the current user",
                                                              "path": "/api/chats/johndoe/11111111-2222-3333-4444-555555555555",
                                                              "method": "GET"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Chat not found",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "NotFoundExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 404,
                                                              "error": "Not Found",
                                                              "message": "Chat not found",
                                                              "path": "/api/chats/johndoe/11111111-2222-3333-4444-555555555555",
                                                              "method": "GET"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    public ResponseEntity<?> getChatDetails(@PathVariable String username,
                                            @PathVariable UUID chatId) {
        ChatResponseDto chat = chatService.getChatDetails(username, chatId);
        return ResponseEntity.status(HttpStatus.OK).body(chat);
    }

    @GetMapping("/{username}/unread")
    @Operation(
            summary = "Retrieve unread chats info",
            description = "Provides aggregated information about unread messages across all chats for the specified user, " +
                    "including details per chat (chat ID and unread count) and the total number of unread messages. " +
                    "The authenticated user must match the provided username.",
            parameters = {
                    @io.swagger.v3.oas.annotations.Parameter(
                            name = "username",
                            description = "Username for which to retrieve unread chats info",
                            required = true
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Unread chats info retrieved successfully",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = UnreadChatsInfoDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Access denied. The authenticated user is not authorized to view this information",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                    name = "ForbiddenExample",
                                                    value = """
                                                            {
                                                              "timestamp": "2025-01-01T12:00:00.123",
                                                              "status": 403,
                                                              "error": "Forbidden",
                                                              "message": "You can only view your own unread info",
                                                              "path": "/api/chats/johndoe/unread",
                                                              "method": "GET"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    public ResponseEntity<?> getUnreadChatsInfo(@PathVariable String username) {
        UnreadChatsInfoDto info = chatService.getUnreadInfo(username);
        return ResponseEntity.status(HttpStatus.OK).body(info);
    }
}
