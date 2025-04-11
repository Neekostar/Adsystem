package com.neekostar.adsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neekostar.adsystem.dto.ChatResponseDto;
import com.neekostar.adsystem.dto.UnreadChatsInfoDto;
import com.neekostar.adsystem.exception.AccessDeniedException;
import com.neekostar.adsystem.exception.GlobalExceptionHandler;
import com.neekostar.adsystem.exception.ResourceNotFoundException;
import com.neekostar.adsystem.model.Chat;
import com.neekostar.adsystem.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class ChatControllerTest {

    private static final String API_CHATS = "/api/chats";
    private static final String TEST_USERNAME = "testuser";
    private static final String OTHER_USERNAME = "otheruser";

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ChatService chatService;

    @InjectMocks
    private ChatController chatController;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        mockMvc = MockMvcBuilders.standaloneSetup(chatController)
                .setControllerAdvice(new GlobalExceptionHandler(messageSource))
                .build();
    }

    @Test
    @DisplayName("GET /api/chats/{username} - success")
    void getAllChatsForCurrentUser_Success() throws Exception {
        ChatResponseDto chatDto = new ChatResponseDto();
        chatDto.setChatId(UUID.randomUUID());
        chatDto.setUsername1(TEST_USERNAME);
        chatDto.setUsername2(OTHER_USERNAME);
        chatDto.setUnreadMessagesCount(3);
        when(chatService.getAllChatsForUser(TEST_USERNAME)).thenReturn(List.of(chatDto));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(TEST_USERNAME, null));

        mockMvc.perform(get(API_CHATS + "/{username}", TEST_USERNAME).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username1", is(TEST_USERNAME)));
    }

    @Test
    @DisplayName("GET /api/chats/{username} - access denied")
    void getAllChatsForCurrentUser_AccessDenied() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("other", "password"));
        when(chatService.getAllChatsForUser(eq(TEST_USERNAME)))
                .thenThrow(new AccessDeniedException("You can only access your own chats"));

        mockMvc.perform(get(API_CHATS + "/{username}", TEST_USERNAME).with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("You can only access your own chats")));
    }

    @Test
    @DisplayName("GET /api/chats/{username}/unread-count - success")
    void getUnreadChatsCount_Success() throws Exception {
        when(chatService.getUnreadChatsCount(TEST_USERNAME)).thenReturn(5);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(TEST_USERNAME, null));

        mockMvc.perform(get(API_CHATS + "/{username}/unread-count", TEST_USERNAME).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(5)));
    }

    @Test
    @DisplayName("GET /api/chats/{username}/unread-count - access denied")
    void getUnreadChatsCount_AccessDenied() throws Exception {
        when(chatService.getUnreadChatsCount(TEST_USERNAME))
                .thenThrow(new AccessDeniedException("You can only access your own chats"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("otherUser", null));

        mockMvc.perform(get(API_CHATS + "/{username}/unread-count", TEST_USERNAME).with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("You can only access your own chats")));
    }

    @Test
    @DisplayName("POST /api/chats/{username}/create/{otherUsername} - create new chat")
    void createChat_Success_NewChat() throws Exception {
        UUID chatId = UUID.randomUUID();
        Chat chat = new Chat();
        chat.setId(chatId);
        when(chatService.getOrCreateChat(TEST_USERNAME, OTHER_USERNAME)).thenReturn(chat);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(TEST_USERNAME, null));

        mockMvc.perform(post(API_CHATS + "/{username}/create/{otherUsername}", TEST_USERNAME, OTHER_USERNAME).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", is(chatId.toString())));
    }

    @Test
    @DisplayName("POST /api/chats/{username}/create/{otherUsername} - self chat error")
    void createChat_SelfChat_Error() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(TEST_USERNAME, null));
        when(chatService.getOrCreateChat(TEST_USERNAME, TEST_USERNAME))
                .thenThrow(new IllegalArgumentException("Users cannot create a chat with themselves"));

        mockMvc.perform(post(API_CHATS + "/{username}/create/{otherUsername}", TEST_USERNAME, TEST_USERNAME).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Users cannot create a chat with themselves")));
    }

    @Test
    @DisplayName("POST /api/chats/{username}/create/{otherUsername} - auth mismatch")
    void createChat_AuthMismatch() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("otheruser", null));
        when(chatService.getOrCreateChat(TEST_USERNAME, OTHER_USERNAME))
                .thenThrow(new AccessDeniedException("You can only create a chat on your own behalf"));

        mockMvc.perform(post(API_CHATS + "/{username}/create/{otherUsername}", TEST_USERNAME, OTHER_USERNAME).with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("You can only create a chat on your own behalf")));
    }

    @Test
    @DisplayName("POST /api/chats/{username}/create/{otherUsername} - resource not found")
    void createChat_ResourceNotFound() throws Exception {
        when(chatService.getOrCreateChat(eq(TEST_USERNAME), eq("nonexistent")))
                .thenThrow(new ResourceNotFoundException("User", "username", "nonexistent"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(TEST_USERNAME, "password"));

        mockMvc.perform(post(API_CHATS + "/{username}/create/{otherUsername}", TEST_USERNAME, "nonexistent").with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("User not found with username : 'nonexistent'")));
    }

    @Test
    @DisplayName("GET /api/chats/{username}/{chatId} - success")
    void getChatDetails_Success() throws Exception {
        UUID chatId = UUID.randomUUID();
        ChatResponseDto dto = new ChatResponseDto();
        dto.setChatId(chatId);
        dto.setUsername1(TEST_USERNAME);
        dto.setUsername2(OTHER_USERNAME);
        dto.setUnreadMessagesCount(0);
        when(chatService.getChatDetails(TEST_USERNAME, chatId)).thenReturn(dto);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(TEST_USERNAME, null));

        mockMvc.perform(get(API_CHATS + "/{username}/{chatId}", TEST_USERNAME, chatId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chatId", is(chatId.toString())));
    }

    @Test
    @DisplayName("GET /api/chats/{username}/{chatId} - access denied")
    void getChatDetails_AccessDenied() throws Exception {
        UUID chatId = UUID.randomUUID();
        when(chatService.getChatDetails(TEST_USERNAME, chatId))
                .thenThrow(new AccessDeniedException("You can only access your own chats"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("otheruser", null));

        mockMvc.perform(get(API_CHATS + "/{username}/{chatId}", TEST_USERNAME, chatId).with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("You can only access your own chats")));
    }

    @Test
    @DisplayName("GET /api/chats/{username}/{chatId} - resource not found")
    void getChatDetails_ResourceNotFound() throws Exception {
        UUID chatId = UUID.randomUUID();
        when(chatService.getChatDetails(TEST_USERNAME, chatId))
                .thenThrow(new ResourceNotFoundException("Chat", "id", chatId.toString()));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(TEST_USERNAME, null));

        mockMvc.perform(get(API_CHATS + "/{username}/{chatId}", TEST_USERNAME, chatId).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/chats/{username}/unread - success")
    void getUnreadChatsInfo_Success() throws Exception {
        UnreadChatsInfoDto info = new UnreadChatsInfoDto();
        info.setTotalUnread(4);
        when(chatService.getUnreadInfo(TEST_USERNAME)).thenReturn(info);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(TEST_USERNAME, null));

        mockMvc.perform(get(API_CHATS + "/{username}/unread", TEST_USERNAME).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUnread", is(4)));
    }

    @Test
    @DisplayName("GET /api/chats/{username}/unread - access denied")
    void getUnreadChatsInfo_AccessDenied() throws Exception {
        when(chatService.getUnreadInfo(TEST_USERNAME))
                .thenThrow(new AccessDeniedException("You can only view your own unread info"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("otheruser", null));

        mockMvc.perform(get(API_CHATS + "/{username}/unread", TEST_USERNAME).with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("You can only view your own unread info")));
    }
}
