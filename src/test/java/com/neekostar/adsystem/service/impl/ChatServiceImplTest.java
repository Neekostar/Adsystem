package com.neekostar.adsystem.service.impl;

import com.neekostar.adsystem.dto.ChatResponseDto;
import com.neekostar.adsystem.dto.ChatUnreadDto;
import com.neekostar.adsystem.dto.UnreadChatsInfoDto;
import com.neekostar.adsystem.exception.AccessDeniedException;
import com.neekostar.adsystem.exception.ResourceNotFoundException;
import com.neekostar.adsystem.mapper.ChatMapper;
import com.neekostar.adsystem.model.Chat;
import com.neekostar.adsystem.model.Message;
import com.neekostar.adsystem.model.User;
import com.neekostar.adsystem.repository.ChatRepository;
import com.neekostar.adsystem.repository.UserRepository;
import com.neekostar.adsystem.service.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ChatServiceImplTest {

    @Mock
    private ChatRepository chatRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ChatMapper chatMapper;
    @Mock
    private EncryptionService encryptionService;
    @Mock
    private Authentication authentication;
    @InjectMocks
    private ChatServiceImpl chatService;

    private User initiator;
    private User other;
    private Chat existingChat;
    private Chat newChat;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));

        initiator = new User();
        initiator.setUsername("initiator");

        other = new User();
        other.setUsername("other");

        existingChat = new Chat();
        existingChat.setId(UUID.randomUUID());
        existingChat.setUser1(initiator);
        existingChat.setUser2(other);

        newChat = new Chat();
        newChat.setId(UUID.randomUUID());
        newChat.setUser1(initiator);
        newChat.setUser2(other);

        ChatResponseDto chatResponseDto = new ChatResponseDto();
        chatResponseDto.setChatId(existingChat.getId());
        chatResponseDto.setUsername1("initiator");
        chatResponseDto.setUsername2("other");
        chatResponseDto.setUnreadMessagesCount(0);
    }

    @Test
    void getOrCreateChat_SameUser_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                chatService.getOrCreateChat("user", "user"));
    }

    @Test
    void getOrCreateChat_AuthMismatch_ThrowsAccessDenied() {
        when(authentication.getName()).thenReturn("differentUser");
        assertThrows(AccessDeniedException.class, () ->
                chatService.getOrCreateChat("initiator", "other"));
    }

    @Test
    void getOrCreateChat_InitiatorNotFound_ThrowsResourceNotFound() {
        when(authentication.getName()).thenReturn("initiator");
        when(userRepository.findUserByUsername("initiator")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () ->
                chatService.getOrCreateChat("initiator", "other"));
    }

    @Test
    void getOrCreateChat_OtherNotFound_ThrowsResourceNotFound() {
        when(authentication.getName()).thenReturn("initiator");
        when(userRepository.findUserByUsername("initiator")).thenReturn(Optional.of(initiator));
        when(userRepository.findUserByUsername("other")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () ->
                chatService.getOrCreateChat("initiator", "other"));
    }

    @Test
    void getOrCreateChat_ExistingChatFirstOrder() {
        when(authentication.getName()).thenReturn("initiator");
        when(userRepository.findUserByUsername("initiator")).thenReturn(Optional.of(initiator));
        when(userRepository.findUserByUsername("other")).thenReturn(Optional.of(other));
        when(chatRepository.findByUser1AndUser2(initiator, other)).thenReturn(Optional.of(existingChat));
        when(chatRepository.findByUser2AndUser1(any(), any())).thenReturn(Optional.empty());

        Chat result = chatService.getOrCreateChat("initiator", "other");
        assertEquals(existingChat, result);
        verify(chatRepository, never()).findByUser2AndUser1(any(), any());
    }

    @Test
    void getOrCreateChat_ExistingChatSecondOrder() {
        when(authentication.getName()).thenReturn("initiator");
        when(userRepository.findUserByUsername("initiator")).thenReturn(Optional.of(initiator));
        when(userRepository.findUserByUsername("other")).thenReturn(Optional.of(other));
        when(chatRepository.findByUser1AndUser2(initiator, other)).thenReturn(Optional.empty());
        when(chatRepository.findByUser2AndUser1(initiator, other)).thenReturn(Optional.of(existingChat));

        Chat result = chatService.getOrCreateChat("initiator", "other");
        assertEquals(existingChat, result);
    }

    @Test
    void getOrCreateChat_NewChatCreated() {
        when(authentication.getName()).thenReturn("initiator");
        when(userRepository.findUserByUsername("initiator")).thenReturn(Optional.of(initiator));
        when(userRepository.findUserByUsername("other")).thenReturn(Optional.of(other));
        when(chatRepository.findByUser1AndUser2(initiator, other)).thenReturn(Optional.empty());
        when(chatRepository.findByUser2AndUser1(initiator, other)).thenReturn(Optional.empty());
        when(chatRepository.saveAndFlush(any(Chat.class))).thenReturn(newChat);

        Chat result = chatService.getOrCreateChat("initiator", "other");
        assertEquals(newChat, result);
    }

    @Test
    void getAllChatsForUser_AccessDenied() {
        when(authentication.getName()).thenReturn("otherUser");
        assertThrows(AccessDeniedException.class, () -> chatService.getAllChatsForUser("initiator"));
    }

    @Test
    void getAllChatsForUser_UserNotFound() {
        when(authentication.getName()).thenReturn("initiator");
        when(userRepository.findUserByUsername("initiator")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> chatService.getAllChatsForUser("initiator"));
    }

    @Test
    void getAllChatsForUser_Success_DecryptionSuccess() {
        when(authentication.getName()).thenReturn("initiator");
        when(userRepository.findUserByUsername("initiator")).thenReturn(Optional.of(initiator));

        Chat chat = new Chat();
        chat.setId(UUID.randomUUID());
        chat.setUser1(initiator);
        chat.setUser2(other);
        Message dummyMessage = mock(Message.class);
        when(dummyMessage.getEncryptedContent()).thenReturn("encrypted");
        when(dummyMessage.getSender()).thenReturn(other);
        List<Message> messages = new ArrayList<>();
        messages.add(dummyMessage);
        chat.setMessages(messages);

        when(encryptionService.decrypt("encrypted")).thenReturn("decrypted");
        ChatResponseDto dto = new ChatResponseDto();
        dto.setChatId(chat.getId());
        dto.setUsername1("initiator");
        dto.setUsername2("other");
        dto.setUnreadMessagesCount(0);
        when(chatMapper.toDto(chat)).thenReturn(dto);
        when(chatRepository.findByUser1OrUser2(any(User.class), any(User.class))).thenReturn(List.of(chat));

        List<ChatResponseDto> result = chatService.getAllChatsForUser("initiator");
        assertEquals(1, result.size());
        assertEquals(dto, result.get(0));
        verify(encryptionService).decrypt("encrypted");
    }

    @Test
    void getAllChatsForUser_DecryptionFailure() {
        when(authentication.getName()).thenReturn("initiator");
        when(userRepository.findUserByUsername("initiator")).thenReturn(Optional.of(initiator));

        Chat chat = new Chat();
        chat.setId(UUID.randomUUID());
        chat.setUser1(initiator);
        chat.setUser2(other);
        Message dummyMessage = mock(Message.class);
        when(dummyMessage.getEncryptedContent()).thenReturn("fail");
        when(dummyMessage.getSender()).thenReturn(other);
        List<Message> messages = new ArrayList<>();
        messages.add(dummyMessage);
        chat.setMessages(messages);

        when(encryptionService.decrypt("fail")).thenThrow(new RuntimeException("decryption error"));
        ChatResponseDto dto = new ChatResponseDto();
        dto.setChatId(chat.getId());
        dto.setUsername1("initiator");
        dto.setUsername2("other");
        dto.setUnreadMessagesCount(0);
        when(chatMapper.toDto(chat)).thenReturn(dto);
        when(chatRepository.findByUser1OrUser2(any(User.class), any(User.class))).thenReturn(List.of(chat));

        List<ChatResponseDto> result = chatService.getAllChatsForUser("initiator");
        assertEquals(1, result.size());
        assertEquals(dto, result.get(0));
    }

    @Test
    void getChatDetails_ChatNotFound() {
        UUID randomChatId = UUID.randomUUID();
        when(chatRepository.findById(randomChatId)).thenReturn(Optional.empty());
        when(authentication.getName()).thenReturn("initiator");
        assertThrows(ResourceNotFoundException.class, () -> chatService.getChatDetails("initiator", randomChatId));
    }

    @Test
    void getChatDetails_AuthMismatch() {
        UUID randomChatId = UUID.randomUUID();
        Chat chat = new Chat();
        chat.setId(randomChatId);
        chat.setUser1(initiator);
        chat.setUser2(other);
        when(chatRepository.findById(randomChatId)).thenReturn(Optional.of(chat));
        when(authentication.getName()).thenReturn("other");
        assertThrows(AccessDeniedException.class, () -> chatService.getChatDetails("initiator", randomChatId));
    }

    @Test
    void getChatDetails_ChatNotBelonging() {
        UUID randomChatId = UUID.randomUUID();
        Chat chat = new Chat();
        chat.setId(randomChatId);
        User another = new User();
        another.setUsername("another");
        chat.setUser1(another);
        chat.setUser2(other);
        when(chatRepository.findById(randomChatId)).thenReturn(Optional.of(chat));
        when(authentication.getName()).thenReturn("initiator");
        assertThrows(AccessDeniedException.class, () -> chatService.getChatDetails("initiator", randomChatId));
    }

    @Test
    void getChatDetails_Success_DecryptionSuccess() {
        UUID randomChatId = UUID.randomUUID();
        Chat chat = new Chat();
        chat.setId(randomChatId);
        chat.setUser1(initiator);
        chat.setUser2(other);
        Message dummyMessage = mock(Message.class);
        when(dummyMessage.getEncryptedContent()).thenReturn("encryptedMsg");
        when(dummyMessage.getSender()).thenReturn(other);
        List<Message> messages = new ArrayList<>();
        messages.add(dummyMessage);
        chat.setMessages(messages);

        when(chatRepository.findById(randomChatId)).thenReturn(Optional.of(chat));
        when(authentication.getName()).thenReturn("initiator");
        when(encryptionService.decrypt("encryptedMsg")).thenReturn("decryptedMsg");
        ChatResponseDto dto = new ChatResponseDto();
        dto.setChatId(randomChatId);
        dto.setUsername1("initiator");
        dto.setUsername2("other");
        dto.setUnreadMessagesCount(0);
        when(chatMapper.toDto(chat)).thenReturn(dto);

        ChatResponseDto result = chatService.getChatDetails("initiator", randomChatId);
        assertEquals(dto, result);
        verify(encryptionService).decrypt("encryptedMsg");
    }

    @Test
    void getChatDetails_DecryptionFailure() {
        UUID randomChatId = UUID.randomUUID();
        Chat chat = new Chat();
        chat.setId(randomChatId);
        chat.setUser1(initiator);
        chat.setUser2(other);
        Message dummyMessage = mock(Message.class);
        when(dummyMessage.getEncryptedContent()).thenReturn("failMsg");
        when(dummyMessage.getSender()).thenReturn(other);
        List<Message> messages = new ArrayList<>();
        messages.add(dummyMessage);
        chat.setMessages(messages);

        when(chatRepository.findById(randomChatId)).thenReturn(Optional.of(chat));
        when(authentication.getName()).thenReturn("initiator");
        when(encryptionService.decrypt("failMsg")).thenThrow(new RuntimeException("decrypt error"));
        ChatResponseDto dto = new ChatResponseDto();
        dto.setChatId(randomChatId);
        dto.setUsername1("initiator");
        dto.setUsername2("other");
        dto.setUnreadMessagesCount(0);
        when(chatMapper.toDto(chat)).thenReturn(dto);

        ChatResponseDto result = chatService.getChatDetails("initiator", randomChatId);
        assertEquals(dto, result);
    }

    @Test
    void getUnreadChatsCount_AccessDenied() {
        when(authentication.getName()).thenReturn("other");
        assertThrows(AccessDeniedException.class, () -> chatService.getUnreadChatsCount("initiator"));
    }

    @Test
    void getUnreadChatsCount_UserNotFound() {
        when(authentication.getName()).thenReturn("initiator");
        when(userRepository.findUserByUsername("initiator")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> chatService.getUnreadChatsCount("initiator"));
    }

    @Test
    void getUnreadChatsCount_Success() {
        when(authentication.getName()).thenReturn("initiator");
        when(userRepository.findUserByUsername("initiator")).thenReturn(Optional.of(initiator));

        Chat chat1 = spy(new Chat());
        chat1.setId(UUID.randomUUID());
        chat1.setUser1(initiator);
        chat1.setUser2(other);
        doNothing().when(chat1).calculateUnreadMessagesFor("initiator");
        when(chat1.getUnreadMessagesCount()).thenReturn(3);

        Chat chat2 = spy(new Chat());
        chat2.setId(UUID.randomUUID());
        chat2.setUser1(other);
        chat2.setUser2(initiator);
        doNothing().when(chat2).calculateUnreadMessagesFor("initiator");
        when(chat2.getUnreadMessagesCount()).thenReturn(0);

        when(chatRepository.findByUser1OrUser2(initiator, initiator)).thenReturn(List.of(chat1, chat2));

        int count = chatService.getUnreadChatsCount("initiator");
        assertEquals(1, count);
    }

    @Test
    void getUnreadInfo_AccessDenied() {
        when(authentication.getName()).thenReturn("other");
        assertThrows(AccessDeniedException.class, () -> chatService.getUnreadInfo("initiator"));
    }

    @Test
    void getUnreadInfo_UserNotFound() {
        when(authentication.getName()).thenReturn("initiator");
        when(userRepository.findUserByUsername("initiator")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> chatService.getUnreadInfo("initiator"));
    }

    @Test
    void getUnreadInfo_Success() {
        when(authentication.getName()).thenReturn("initiator");
        when(userRepository.findUserByUsername("initiator")).thenReturn(Optional.of(initiator));

        Chat chat1 = spy(new Chat());
        UUID chat1Id = UUID.randomUUID();
        chat1.setId(chat1Id);
        chat1.setUser1(initiator);
        chat1.setUser2(other);
        doNothing().when(chat1).calculateUnreadMessagesFor("initiator");
        when(chat1.getUnreadMessagesCount()).thenReturn(2);

        Chat chat2 = spy(new Chat());
        UUID chat2Id = UUID.randomUUID();
        chat2.setId(chat2Id);
        chat2.setUser1(other);
        chat2.setUser2(initiator);
        doNothing().when(chat2).calculateUnreadMessagesFor("initiator");
        when(chat2.getUnreadMessagesCount()).thenReturn(3);

        when(chatRepository.findByUser1OrUser2(initiator, initiator)).thenReturn(List.of(chat1, chat2));

        UnreadChatsInfoDto info = chatService.getUnreadInfo("initiator");
        assertEquals(5, info.getTotalUnread());
        assertEquals(2, info.getChats().size());
        for (ChatUnreadDto chatUnread : info.getChats()) {
            if (chatUnread.getChatId().equals(chat1Id)) {
                assertEquals(2, chatUnread.getUnreadCount());
            } else if (chatUnread.getChatId().equals(chat2Id)) {
                assertEquals(3, chatUnread.getUnreadCount());
            } else {
                fail("Unexpected chat id in unread info");
            }
        }
    }
}
