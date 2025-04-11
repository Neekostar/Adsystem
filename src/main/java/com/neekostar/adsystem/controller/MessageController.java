package com.neekostar.adsystem.controller;

import java.util.List;
import java.util.UUID;
import com.neekostar.adsystem.dto.ErrorResponse;
import com.neekostar.adsystem.dto.MessageCreateDto;
import com.neekostar.adsystem.dto.MessageResponseDto;
import com.neekostar.adsystem.dto.MessageUpdateDto;
import com.neekostar.adsystem.service.MessageService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
@Tag(
        name = "Message Management",
        description = "This controller handles all operations related to messaging between users. <br><br>" +
                "The available operations include: <ul>" +
                "<li><b>Retrieve Chat Messages</b> – Fetch all messages in a specific chat. The authenticated user " +
                "must be a participant of the chat. Each message's content is decrypted before being returned.</li>" +
                "<li><b>Send Message</b> – Sends a new message within a chat. The message text is encrypted before " +
                "storage. Only the authenticated user (acting as the sender) is permitted to send messages in a chat " +
                "they belong to.</li>" +
                "<li><b>Mark Message as Read</b> – Marks a specific message as read. This operation ensures that " +
                "only the intended recipient can mark the message as read.</li>" +
                "<li><b>Mark All Messages as Read</b> – Marks all unread messages in a specific chat as read " +
                "for the provided user. The authenticated user must match the user for whom the messages are being marked.</li>" +
                "<li><b>Update Message</b> – Updates the content of an existing message. Only the sender of the message " +
                "is authorized to update it. The new message content is encrypted before being saved.</li>" +
                "<li><b>Delete Message</b> – Deletes an existing message. Only the sender is allowed to delete their own messages.</li>" +
                "</ul><br>" +
                "Possible exceptions include: <br><br>" +
                "<b>ResourceNotFoundException</b> – when the chat, message, or user is not found; <br>" +
                "<b>AccessDeniedException</b> – when the authenticated user is not authorized to perform the requested operation."
)
public class MessageController {

    private final MessageService messageService;

    @Autowired
    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/{username}/{chatId}")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Retrieve all messages for a chat",
            description = "Fetches all messages for the specified chat identified by its UUID. " +
                    "The authenticated user must match the provided username and be a participant of the chat. " +
                    "All messages are returned with decrypted content. " +
                    "Possible errors: 403 if access is denied, 404 if the chat is not found.",
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
                            description = "Messages retrieved successfully",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = MessageResponseDto.class)
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Access denied. The authenticated user is not authorized to access these messages",
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
                                                              "message": "You can only access your own messages",
                                                              "path": "/api/messages/johndoe/11111111-2222-3333-4444-555555555555",
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
                                                              "path": "/api/messages/johndoe/11111111-2222-3333-4444-555555555555",
                                                              "method": "GET"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    public ResponseEntity<?> getChatMessages(@PathVariable String username,
                                             @PathVariable UUID chatId) {
        List<MessageResponseDto> messages = messageService.getMessagesForChat(username, chatId);
        return ResponseEntity.status(HttpStatus.OK).body(messages);
    }

    @PostMapping("/{username}/{chatId}")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Send a new message",
            description = "Sends a new message in the specified chat. The authenticated user must match the provided username " +
                    "and be a participant of the chat. The message text is encrypted before being saved. " +
                    "On success, returns the created message details. " +
                    "Possible errors: 403 if access is denied, 404 if the chat or sender is not found.",
            parameters = {
                    @io.swagger.v3.oas.annotations.Parameter(
                            name = "username",
                            description = "Username of the sender (must match the authenticated user)",
                            required = true
                    ),
                    @io.swagger.v3.oas.annotations.Parameter(
                            name = "chatId",
                            description = "Unique identifier (UUID) of the chat",
                            required = true
                    )
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Message creation data containing the message text",
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = MessageCreateDto.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Message sent successfully",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = MessageResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Access denied. The authenticated user is not authorized to send messages on behalf of another user or is not a participant of the chat",
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
                                                              "message": "You can only send messages on your own behalf",
                                                              "path": "/api/messages/johndoe/11111111-2222-3333-4444-555555555555",
                                                              "method": "POST"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Chat or sender not found",
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
                                                              "path": "/api/messages/johndoe/11111111-2222-3333-4444-555555555555",
                                                              "method": "POST"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    public ResponseEntity<?> sendMessage(
            @PathVariable String username,
            @PathVariable UUID chatId,
            @Validated @RequestBody MessageCreateDto dto
    ) {
        MessageResponseDto message = messageService.sendMessage(chatId, username, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    @PostMapping("/{username}/read/{messageId}")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Mark a message as read",
            description = "Marks the specified message as read. The authenticated user must match the provided username " +
                    "and be the recipient of the message. " +
                    "Possible errors: 403 if the user is not the recipient, 404 if the message is not found.",
            parameters = {
                    @io.swagger.v3.oas.annotations.Parameter(
                            name = "username",
                            description = "Username of the recipient marking the message as read",
                            required = true
                    ),
                    @io.swagger.v3.oas.annotations.Parameter(
                            name = "messageId",
                            description = "Unique identifier (UUID) of the message to mark as read",
                            required = true
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Message marked as read successfully"
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Access denied. The user is not the recipient of the message",
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
                                                              "message": "You can only mark your own messages as read",
                                                              "path": "/api/messages/johndoe/read/11111111-2222-3333-4444-555555555555",
                                                              "method": "POST"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Message not found",
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
                                                              "message": "Message not found",
                                                              "path": "/api/messages/johndoe/read/11111111-2222-3333-4444-555555555555",
                                                              "method": "POST"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    public ResponseEntity<Void> markMessageAsRead(@PathVariable String username,
                                                  @PathVariable UUID messageId) {
        messageService.markMessageAsRead(username, messageId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/{username}/{chatId}/read-all")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Mark all messages in a chat as read",
            description = "Marks all unread messages as read in the specified chat for the given user. " +
                    "The authenticated user must match the provided username and be a participant of the chat. " +
                    "Possible errors: 403 if access is denied, 404 if the chat is not found.",
            parameters = {
                    @io.swagger.v3.oas.annotations.Parameter(
                            name = "username",
                            description = "Username for which to mark messages as read",
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
                            description = "All messages marked as read successfully"
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Access denied. The user is not authorized to mark these messages as read",
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
                                                              "message": "You can only mark your own incoming messages as read",
                                                              "path": "/api/messages/johndoe/11111111-2222-3333-4444-555555555555/read-all",
                                                              "method": "POST"
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
                                                              "path": "/api/messages/johndoe/11111111-2222-3333-4444-555555555555/read-all",
                                                              "method": "POST"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    public ResponseEntity<Void> markAllMessagesAsRead(@PathVariable String username,
                                                      @PathVariable UUID chatId) {
        messageService.markAllMessagesAsRead(username, chatId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PutMapping("/{username}/{messageId}")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Update an existing message",
            description = "Updates the content of a specified message. " +
                    "The authenticated user must be the sender of the message. " +
                    "The new message text is encrypted and updated. " +
                    "Possible errors: 403 if the user is not the sender, 404 if the message is not found.",
            parameters = {
                    @io.swagger.v3.oas.annotations.Parameter(
                            name = "username",
                            description = "Username of the sender (must match the authenticated user)",
                            required = true
                    ),
                    @io.swagger.v3.oas.annotations.Parameter(
                            name = "messageId",
                            description = "Unique identifier (UUID) of the message to update",
                            required = true
                    )
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Message update data containing the new message text",
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = MessageUpdateDto.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Message updated successfully",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    mediaType = "application/json",
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = MessageResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Access denied. The authenticated user is not the sender of the message",
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
                                                              "message": "You can only update your own messages",
                                                              "path": "/api/messages/johndoe/11111111-2222-3333-4444-555555555555",
                                                              "method": "PUT"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Message not found",
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
                                                              "message": "Message not found",
                                                              "path": "/api/messages/johndoe/11111111-2222-3333-4444-555555555555",
                                                              "method": "PUT"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    public ResponseEntity<MessageResponseDto> updateMessage(
            @PathVariable String username,
            @PathVariable UUID messageId,
            @Validated @RequestBody MessageUpdateDto dto
    ) {
        MessageResponseDto message = messageService.updateMessage(username, messageId, dto);
        return ResponseEntity.status(HttpStatus.OK).body(message);
    }

    @DeleteMapping("/{username}/{messageId}")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Delete a message",
            description = "Deletes the specified message. " +
                    "The authenticated user must be the sender of the message. " +
                    "Possible errors: 403 if the user is not the sender, 404 if the message is not found.",
            parameters = {
                    @io.swagger.v3.oas.annotations.Parameter(
                            name = "username",
                            description = "Username of the sender (must match the authenticated user)",
                            required = true
                    ),
                    @io.swagger.v3.oas.annotations.Parameter(
                            name = "messageId",
                            description = "Unique identifier (UUID) of the message to delete",
                            required = true
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "Message deleted successfully"
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Access denied. The authenticated user is not the sender of the message",
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
                                                              "message": "You can only delete your own messages",
                                                              "path": "/api/messages/johndoe/11111111-2222-3333-4444-555555555555",
                                                              "method": "DELETE"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Message not found",
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
                                                              "message": "Message not found",
                                                              "path": "/api/messages/johndoe/11111111-2222-3333-4444-555555555555",
                                                              "method": "DELETE"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    public ResponseEntity<Void> deleteMessage(@PathVariable String username,
                                              @PathVariable UUID messageId) {
        messageService.deleteMessage(username, messageId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
