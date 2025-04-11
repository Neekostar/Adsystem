package com.neekostar.adsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neekostar.adsystem.dto.MessageCreateDto;
import com.neekostar.adsystem.dto.MessageResponseDto;
import com.neekostar.adsystem.dto.MessageUpdateDto;
import com.neekostar.adsystem.exception.AccessDeniedException;
import com.neekostar.adsystem.exception.GlobalExceptionHandler;
import com.neekostar.adsystem.exception.ResourceNotFoundException;
import com.neekostar.adsystem.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class MessageControllerTest {

    private static final String API_MESSAGES = "/api/messages";
    private static final String TEST_USERNAME = "senderUser";
    private static final UUID TEST_CHAT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TEST_MESSAGE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private MessageService messageService;

    @InjectMocks
    private MessageController messageController;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        mockMvc = MockMvcBuilders.standaloneSetup(messageController)
                .setControllerAdvice(new GlobalExceptionHandler(messageSource))
                .build();
    }

    @Test
    @DisplayName("GET /api/messages/{username}/{chatId} - success")
    void getChatMessages_Success() throws Exception {
        MessageResponseDto dto = new MessageResponseDto();
        dto.setMessageId(TEST_MESSAGE_ID);
        dto.setChatId(TEST_CHAT_ID);
        dto.setSenderUsername(TEST_USERNAME);
        dto.setRecipientUsername("recipientUser");
        dto.setContent("Decrypted message");

        when(messageService.getMessagesForChat(eq(TEST_USERNAME), eq(TEST_CHAT_ID)))
                .thenReturn(List.of(dto));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TEST_USERNAME, "password"));

        mockMvc.perform(get(API_MESSAGES + "/{username}/{chatId}", TEST_USERNAME, TEST_CHAT_ID)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].messageId", is(TEST_MESSAGE_ID.toString())))
                .andExpect(jsonPath("$[0].senderUsername", is(TEST_USERNAME)));
    }

    @Test
    @DisplayName("GET /api/messages/{username}/{chatId} - access denied")
    void getChatMessages_AccessDenied() throws Exception {
        when(messageService.getMessagesForChat(eq(TEST_USERNAME), eq(TEST_CHAT_ID)))
                .thenThrow(new AccessDeniedException("You can only access your own messages"));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("otherUser", "password"));

        mockMvc.perform(get(API_MESSAGES + "/{username}/{chatId}", TEST_USERNAME, TEST_CHAT_ID)
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("You can only access your own messages")));
    }

    @Test
    @DisplayName("GET /api/messages/{username}/{chatId} - chat not found")
    void getChatMessages_ChatNotFound() throws Exception {
        when(messageService.getMessagesForChat(eq(TEST_USERNAME), eq(TEST_CHAT_ID)))
                .thenThrow(new ResourceNotFoundException("Chat", "id", TEST_CHAT_ID.toString()));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TEST_USERNAME, "password"));

        mockMvc.perform(get(API_MESSAGES + "/{username}/{chatId}", TEST_USERNAME, TEST_CHAT_ID)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/messages/{username}/{chatId} - send message success")
    void sendMessage_Success() throws Exception {
        MessageCreateDto createDto = new MessageCreateDto("Hello, world!");
        MessageResponseDto responseDto = new MessageResponseDto();
        responseDto.setMessageId(TEST_MESSAGE_ID);
        responseDto.setChatId(TEST_CHAT_ID);
        responseDto.setSenderUsername(TEST_USERNAME);
        responseDto.setRecipientUsername("recipientUser");
        responseDto.setContent("Decrypted: Hello, world!");

        when(messageService.sendMessage(eq(TEST_CHAT_ID), eq(TEST_USERNAME), eq(createDto)))
                .thenReturn(responseDto);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TEST_USERNAME, "password"));

        mockMvc.perform(post(API_MESSAGES + "/{username}/{chatId}", TEST_USERNAME, TEST_CHAT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.messageId", is(TEST_MESSAGE_ID.toString())))
                .andExpect(jsonPath("$.senderUsername", is(TEST_USERNAME)));
    }

    @Test
    @DisplayName("POST /api/messages/{username}/{chatId} - send message access denied")
    void sendMessage_AccessDenied() throws Exception {
        MessageCreateDto createDto = new MessageCreateDto("Hello!");
        when(messageService.sendMessage(eq(TEST_CHAT_ID), eq(TEST_USERNAME), eq(createDto)))
                .thenThrow(new AccessDeniedException("You can only send messages on your own behalf"));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("otherUser", "password"));

        mockMvc.perform(post(API_MESSAGES + "/{username}/{chatId}", TEST_USERNAME, TEST_CHAT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("You can only send messages on your own behalf")));
    }

    @Test
    @DisplayName("POST /api/messages/{username}/{chatId} - send message chat not found")
    void sendMessage_ChatNotFound() throws Exception {
        MessageCreateDto createDto = new MessageCreateDto("Hello!");
        when(messageService.sendMessage(eq(TEST_CHAT_ID), eq(TEST_USERNAME), eq(createDto)))
                .thenThrow(new ResourceNotFoundException("Chat", "id", TEST_CHAT_ID.toString()));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TEST_USERNAME, "password"));

        mockMvc.perform(post(API_MESSAGES + "/{username}/{chatId}", TEST_USERNAME, TEST_CHAT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/messages/{username}/read/{messageId} - mark message as read success")
    void markMessageAsRead_Success() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TEST_USERNAME, "password"));

        mockMvc.perform(post(API_MESSAGES + "/{username}/read/{messageId}", TEST_USERNAME, TEST_MESSAGE_ID)
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/messages/{username}/read/{messageId} - mark message as read access denied")
    void markMessageAsRead_AccessDenied() throws Exception {
        doThrow(new AccessDeniedException("You can only mark your own messages as read"))
                .when(messageService).markMessageAsRead(eq(TEST_USERNAME), eq(TEST_MESSAGE_ID));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("otherUser", "password"));

        mockMvc.perform(post(API_MESSAGES + "/{username}/read/{messageId}", TEST_USERNAME, TEST_MESSAGE_ID)
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("You can only mark your own messages as read")));
    }

    @Test
    @DisplayName("POST /api/messages/{username}/read/{messageId} - mark message as read: message not found")
    void markMessageAsRead_MessageNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Message", "id", TEST_MESSAGE_ID.toString()))
                .when(messageService).markMessageAsRead(eq(TEST_USERNAME), eq(TEST_MESSAGE_ID));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TEST_USERNAME, "password"));

        mockMvc.perform(post(API_MESSAGES + "/{username}/read/{messageId}", TEST_USERNAME, TEST_MESSAGE_ID)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/messages/{username}/{chatId}/read-all - mark all messages as read success")
    void markAllMessagesAsRead_Success() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TEST_USERNAME, "password"));

        mockMvc.perform(post(API_MESSAGES + "/{username}/{chatId}/read-all", TEST_USERNAME, TEST_CHAT_ID)
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/messages/{username}/{chatId}/read-all - mark all messages as read access denied")
    void markAllMessagesAsRead_AccessDenied() throws Exception {
        doThrow(new AccessDeniedException("You can only mark your own incoming messages as read"))
                .when(messageService).markAllMessagesAsRead(eq(TEST_USERNAME), eq(TEST_CHAT_ID));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("otherUser", "password"));

        mockMvc.perform(post(API_MESSAGES + "/{username}/{chatId}/read-all", TEST_USERNAME, TEST_CHAT_ID)
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("You can only mark your own incoming messages as read")));
    }

    @Test
    @DisplayName("POST /api/messages/{username}/{chatId}/read-all - mark all messages as read: chat not found")
    void markAllMessagesAsRead_ChatNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Chat", "id", TEST_CHAT_ID.toString()))
                .when(messageService).markAllMessagesAsRead(eq(TEST_USERNAME), eq(TEST_CHAT_ID));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TEST_USERNAME, "password"));

        mockMvc.perform(post(API_MESSAGES + "/{username}/{chatId}/read-all", TEST_USERNAME, TEST_CHAT_ID)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/messages/{username}/{messageId} - update message success")
    void updateMessage_Success() throws Exception {
        MessageUpdateDto updateDto = new MessageUpdateDto();
        updateDto.setNewMessageText("Updated text");

        MessageResponseDto responseDto = new MessageResponseDto();
        responseDto.setMessageId(TEST_MESSAGE_ID);
        responseDto.setChatId(TEST_CHAT_ID);
        responseDto.setSenderUsername(TEST_USERNAME);
        responseDto.setContent("Decrypted updated text");

        when(messageService.updateMessage(eq(TEST_USERNAME), eq(TEST_MESSAGE_ID), eq(updateDto)))
                .thenReturn(responseDto);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TEST_USERNAME, "password"));

        mockMvc.perform(put(API_MESSAGES + "/{username}/{messageId}", TEST_USERNAME, TEST_MESSAGE_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId", is(TEST_MESSAGE_ID.toString())))
                .andExpect(jsonPath("$.content", is("Decrypted updated text")));
    }

    @Test
    @DisplayName("PUT /api/messages/{username}/{messageId} - update message access denied")
    void updateMessage_AccessDenied() throws Exception {
        MessageUpdateDto updateDto = new MessageUpdateDto();
        updateDto.setNewMessageText("Updated text");

        when(messageService.updateMessage(eq(TEST_USERNAME), eq(TEST_MESSAGE_ID), eq(updateDto)))
                .thenThrow(new AccessDeniedException("You can only update your own messages"));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("otherUser", "password"));

        mockMvc.perform(put(API_MESSAGES + "/{username}/{messageId}", TEST_USERNAME, TEST_MESSAGE_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("You can only update your own messages")));
    }

    @Test
    @DisplayName("PUT /api/messages/{username}/{messageId} - update message not found")
    void updateMessage_NotFound() throws Exception {
        MessageUpdateDto updateDto = new MessageUpdateDto();
        updateDto.setNewMessageText("Updated text");

        when(messageService.updateMessage(eq(TEST_USERNAME), eq(TEST_MESSAGE_ID), eq(updateDto)))
                .thenThrow(new ResourceNotFoundException("Message", "id", TEST_MESSAGE_ID.toString()));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TEST_USERNAME, "password"));

        mockMvc.perform(put(API_MESSAGES + "/{username}/{messageId}", TEST_USERNAME, TEST_MESSAGE_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/messages/{username}/{messageId} - delete message success")
    void deleteMessage_Success() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TEST_USERNAME, "password"));

        mockMvc.perform(delete(API_MESSAGES + "/{username}/{messageId}", TEST_USERNAME, TEST_MESSAGE_ID)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/messages/{username}/{messageId} - delete message access denied")
    void deleteMessage_AccessDenied() throws Exception {
        doThrow(new AccessDeniedException("You can only delete your own messages"))
                .when(messageService).deleteMessage(eq(TEST_USERNAME), eq(TEST_MESSAGE_ID));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("otherUser", "password"));

        mockMvc.perform(delete(API_MESSAGES + "/{username}/{messageId}", TEST_USERNAME, TEST_MESSAGE_ID)
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("You can only delete your own messages")));
    }

    @Test
    @DisplayName("DELETE /api/messages/{username}/{messageId} - delete message not found")
    void deleteMessage_NotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Message", "id", TEST_MESSAGE_ID.toString()))
                .when(messageService).deleteMessage(eq(TEST_USERNAME), eq(TEST_MESSAGE_ID));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TEST_USERNAME, "password"));

        mockMvc.perform(delete(API_MESSAGES + "/{username}/{messageId}", TEST_USERNAME, TEST_MESSAGE_ID)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }
}
