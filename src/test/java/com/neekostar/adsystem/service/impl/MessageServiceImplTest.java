package com.neekostar.adsystem.service.impl;

import com.github.javafaker.Faker;
import com.neekostar.adsystem.dto.MessageCreateDto;
import com.neekostar.adsystem.dto.MessageResponseDto;
import com.neekostar.adsystem.dto.MessageUpdateDto;
import com.neekostar.adsystem.exception.AccessDeniedException;
import com.neekostar.adsystem.exception.ResourceNotFoundException;
import com.neekostar.adsystem.mapper.MessageMapper;
import com.neekostar.adsystem.model.Chat;
import com.neekostar.adsystem.model.Message;
import com.neekostar.adsystem.model.User;
import com.neekostar.adsystem.repository.ChatRepository;
import com.neekostar.adsystem.repository.MessageRepository;
import com.neekostar.adsystem.repository.UserRepository;
import com.neekostar.adsystem.service.EncryptionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MessageServiceImplTest {
    private static final String SENDER_USERNAME = "senderUser";
    private static final String RECEIVER_USERNAME = "receiverUser";

    @Mock
    private ChatRepository chatRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EncryptionService encryptionService;
    @Mock
    private MessageMapper messageMapper;
    @Mock
    private Authentication authentication;
    @InjectMocks
    private MessageServiceImpl messageService;

    private User sender;
    private User receiver;
    private Chat chat;
    private MessageCreateDto createDto;
    private MessageUpdateDto updateDto;
    private MessageResponseDto responseDto;
    private UUID chatId;
    private UUID messageId;

    @BeforeEach
    void setUp() {
        Faker faker = new Faker();
        chatId = UUID.randomUUID();
        messageId = UUID.randomUUID();

        sender = new User();
        sender.setId(UUID.randomUUID());
        sender.setUsername(SENDER_USERNAME);

        receiver = new User();
        receiver.setId(UUID.randomUUID());
        receiver.setUsername(RECEIVER_USERNAME);

        chat = new Chat();
        chat.setId(chatId);
        chat.setUser1(sender);
        chat.setUser2(receiver);

        Message message = new Message();
        message.setId(messageId);
        message.setChat(chat);
        message.setSender(sender);
        message.setRecipient(receiver);
        message.setEncryptedContent("encryptedTestMessage");
        message.setRead(false);
        message.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        message.setUpdatedAt(LocalDateTime.now().minusMinutes(10));

        createDto = new MessageCreateDto(faker.lorem().sentence());

        updateDto = new MessageUpdateDto();
        updateDto.setNewMessageText(faker.lorem().sentence());

        responseDto = new MessageResponseDto();
        responseDto.setChatId(chatId);
        responseDto.setMessageId(messageId);
        responseDto.setSenderUsername(SENDER_USERNAME);
        responseDto.setRecipientUsername(RECEIVER_USERNAME);
        responseDto.setContent("Decrypted message");
        responseDto.setRead(false);
        responseDto.setCreatedAt(message.getCreatedAt());
        responseDto.setUpdatedAt(message.getUpdatedAt());

        SecurityContext context = new SecurityContextImpl(authentication);
        SecurityContextHolder.setContext(context);
        when(authentication.getName()).thenReturn(SENDER_USERNAME);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sendMessage_Success() {
        when(userRepository.findUserByUsername(SENDER_USERNAME)).thenReturn(Optional.of(sender));
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(encryptionService.encrypt(createDto.getMessageText()))
                .thenReturn("encrypted_" + createDto.getMessageText());
        when(messageRepository.saveAndFlush(any(Message.class))).thenAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            msg.setId(messageId);
            return msg;
        });
        when(messageMapper.toDto(any(Message.class))).thenReturn(responseDto);

        MessageResponseDto result = messageService.sendMessage(chatId, SENDER_USERNAME, createDto);

        assertNotNull(result);
        assertEquals(SENDER_USERNAME, result.getSenderUsername());
        assertEquals(RECEIVER_USERNAME, result.getRecipientUsername());
        verify(messageRepository).saveAndFlush(any(Message.class));
    }

    @Test
    void sendMessage_UnauthorizedSender_ShouldThrowAccessDenied() {
        when(authentication.getName()).thenReturn("otherUser");
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                messageService.sendMessage(chatId, SENDER_USERNAME, createDto));
        assertEquals("You can only send messages on your own behalf", exception.getMessage());
    }

    @Test
    void sendMessage_ChatNotFound_ShouldThrowResourceNotFound() {
        when(userRepository.findUserByUsername(SENDER_USERNAME)).thenReturn(Optional.of(sender));
        when(chatRepository.findById(chatId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> messageService.sendMessage(chatId, SENDER_USERNAME, createDto));
        assertTrue(exception.getMessage().contains("Chat"));
    }

    @Test
    void sendMessage_UserNotParticipant_ShouldThrowAccessDenied() {
        Chat chatWithoutSender = new Chat();
        chatWithoutSender.setId(chatId);
        User nonParticipant = new User();
        nonParticipant.setUsername("nonParticipant");
        chatWithoutSender.setUser1(nonParticipant);
        chatWithoutSender.setUser2(receiver);
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chatWithoutSender));
        when(userRepository.findUserByUsername(SENDER_USERNAME)).thenReturn(Optional.of(sender));
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                messageService.sendMessage(chatId, SENDER_USERNAME, createDto));
        assertEquals("You are not a participant of this chat", exception.getMessage());
    }

    @Test
    void sendMessage_SenderNotFound_ShouldThrowResourceNotFound() {
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(userRepository.findUserByUsername(SENDER_USERNAME)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                messageService.sendMessage(chatId, SENDER_USERNAME, createDto));
        assertTrue(exception.getMessage().contains("User"));
    }

    @Test
    void sendMessage_AutoMarkIncomingAsRead() {
        Message unreadMsg = new Message();
        unreadMsg.setId(UUID.randomUUID());
        unreadMsg.setSender(receiver);
        unreadMsg.setRecipient(sender);
        unreadMsg.setRead(false);

        List<Message> messages = new ArrayList<>();
        messages.add(unreadMsg);
        chat.setMessages(messages);

        when(userRepository.findUserByUsername(SENDER_USERNAME)).thenReturn(Optional.of(sender));
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(encryptionService.encrypt(createDto.getMessageText()))
                .thenReturn("encrypted_" + createDto.getMessageText());
        when(messageRepository.saveAndFlush(any(Message.class))).thenAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            msg.setId(messageId);
            return msg;
        });
        when(messageMapper.toDto(any(Message.class))).thenReturn(responseDto);

        MessageResponseDto result = messageService.sendMessage(chatId, SENDER_USERNAME, createDto);
        assertTrue(unreadMsg.isRead());
        verify(messageRepository).saveAllAndFlush(argThat(iterable -> {
            List<Message> list = new ArrayList<>();
            iterable.forEach(list::add);
            return list.contains(unreadMsg);
        }));

        assertNotNull(result);
    }

    @Test
    void getMessagesForChat_Success() {
        when(authentication.getName()).thenReturn(SENDER_USERNAME);
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));

        Message msg1 = new Message();
        msg1.setId(UUID.randomUUID());
        msg1.setChat(chat);
        msg1.setSender(sender);
        msg1.setRecipient(receiver);
        msg1.setEncryptedContent("enc1");
        msg1.setRead(false);
        msg1.setCreatedAt(LocalDateTime.now().minusMinutes(5));

        Message msg2 = new Message();
        msg2.setId(UUID.randomUUID());
        msg2.setChat(chat);
        msg2.setSender(receiver);
        msg2.setRecipient(sender);
        msg2.setEncryptedContent("enc2");
        msg2.setRead(true);
        msg2.setCreatedAt(LocalDateTime.now().minusMinutes(2));

        when(messageRepository.findByChatOrderByCreatedAtAsc(chat))
                .thenReturn(Arrays.asList(msg1, msg2));
        when(encryptionService.decrypt("enc1")).thenReturn("Decrypted msg1");
        when(encryptionService.decrypt("enc2")).thenReturn("Decrypted msg2");

        when(messageMapper.toDto(any(Message.class))).thenAnswer(invocation -> {
            Message m = invocation.getArgument(0);
            MessageResponseDto dto = new MessageResponseDto();
            dto.setChatId(chatId);
            dto.setMessageId(m.getId());
            dto.setSenderUsername(m.getSender().getUsername());
            dto.setRecipientUsername(m.getRecipient().getUsername());
            dto.setContent(m.getPlainContent());
            dto.setRead(m.isRead());
            dto.setCreatedAt(m.getCreatedAt());
            dto.setUpdatedAt(m.getUpdatedAt());
            return dto;
        });

        List<MessageResponseDto> results = messageService.getMessagesForChat(SENDER_USERNAME, chatId);

        assertEquals(2, results.size());
        assertEquals("Decrypted msg1", results.get(0).getContent());
        assertEquals("Decrypted msg2", results.get(1).getContent());
    }

    @Test
    void getMessagesForChat_DecryptionFailure() {
        when(authentication.getName()).thenReturn(SENDER_USERNAME);
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));

        Message msg = new Message();
        msg.setId(UUID.randomUUID());
        msg.setChat(chat);
        msg.setSender(receiver);
        msg.setRecipient(sender);
        msg.setEncryptedContent("failEnc");
        msg.setRead(false);
        msg.setCreatedAt(LocalDateTime.now().minusMinutes(5));

        when(messageRepository.findByChatOrderByCreatedAtAsc(chat))
                .thenReturn(List.of(msg));
        when(encryptionService.decrypt("failEnc")).thenThrow(new RuntimeException("decrypt error"));
        when(messageMapper.toDto(any(Message.class))).thenAnswer(invocation -> {
            Message m = invocation.getArgument(0);
            MessageResponseDto dto = new MessageResponseDto();
            dto.setChatId(chatId);
            dto.setMessageId(m.getId());
            dto.setSenderUsername(m.getSender().getUsername());
            dto.setRecipientUsername(m.getRecipient().getUsername());
            dto.setContent(m.getPlainContent());
            dto.setRead(m.isRead());
            dto.setCreatedAt(m.getCreatedAt());
            dto.setUpdatedAt(m.getUpdatedAt());
            return dto;
        });

        List<MessageResponseDto> results = messageService.getMessagesForChat(SENDER_USERNAME, chatId);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertNull(results.get(0).getContent());
    }

    @Test
    void getMessagesForChat_AuthUserMismatch_ShouldThrowAccessDenied() {
        when(authentication.getName()).thenReturn("otherUser");
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                messageService.getMessagesForChat(SENDER_USERNAME, chatId));
        assertEquals("You can only access your own messages", exception.getMessage());
    }

    @Test
    void getMessagesForChat_ChatNotFound_ShouldThrowResourceNotFound() {
        when(chatRepository.findById(chatId)).thenReturn(Optional.empty());
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                messageService.getMessagesForChat(SENDER_USERNAME, chatId));
        assertTrue(exception.getMessage().contains("Chat"));
    }

    @Test
    void getMessagesForChat_NotParticipant_ShouldThrowAccessDenied() {
        Chat chatNotParticipant = new Chat();
        chatNotParticipant.setId(chatId);

        User nonParticipant = new User();
        nonParticipant.setUsername("nonParticipant");
        chatNotParticipant.setUser1(nonParticipant);
        chatNotParticipant.setUser2(receiver);
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chatNotParticipant));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                messageService.getMessagesForChat(SENDER_USERNAME, chatId));
        assertEquals("You can only access your own chats", exception.getMessage());
    }

    @Test
    void markMessageAsRead_Success() {
        Message msg = new Message();
        msg.setId(UUID.randomUUID());
        msg.setSender(sender);
        msg.setRecipient(receiver);
        msg.setRead(false);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(msg));
        when(authentication.getName()).thenReturn(RECEIVER_USERNAME);
        receiver.setUsername(RECEIVER_USERNAME);

        messageService.markMessageAsRead(RECEIVER_USERNAME, messageId);

        assertTrue(msg.isRead());
        verify(messageRepository).saveAndFlush(msg);
    }

    @Test
    void markMessageAsRead_Unauthorized_ShouldThrowAccessDenied() {
        when(authentication.getName()).thenReturn("otherUser");
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                messageService.markMessageAsRead(RECEIVER_USERNAME, messageId));
        assertEquals("You can only mark your own messages as read", exception.getMessage());
    }

    @Test
    void markMessageAsRead_MessageNotFound_ShouldThrowResourceNotFound() {
        when(authentication.getName()).thenReturn(RECEIVER_USERNAME);
        when(messageRepository.findById(messageId)).thenReturn(Optional.empty());
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                messageService.markMessageAsRead(RECEIVER_USERNAME, messageId));
        assertTrue(exception.getMessage().contains("Message"));
    }

    @Test
    void markMessageAsRead_RecipientMismatch_ShouldThrowAccessDenied() {
        Message msg = new Message();
        msg.setId(messageId);
        msg.setSender(sender);

        User wrongRecipient = new User();
        wrongRecipient.setUsername("differentRecipient");
        msg.setRecipient(wrongRecipient);
        msg.setRead(false);

        when(messageRepository.findById(messageId)).thenReturn(Optional.of(msg));
        when(authentication.getName()).thenReturn("correctRecipient");

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> messageService.markMessageAsRead("correctRecipient", messageId));
        assertEquals("You can only mark your own messages as read", exception.getMessage());
    }

    @Test
    void markAllMessagesAsRead_Success() {
        when(authentication.getName()).thenReturn(SENDER_USERNAME);

        Message m1 = new Message();
        m1.setId(UUID.randomUUID());
        m1.setSender(receiver);
        m1.setRecipient(sender);
        m1.setRead(false);

        Message m2 = new Message();
        m2.setId(UUID.randomUUID());
        m2.setSender(receiver);
        m2.setRecipient(sender);
        m2.setRead(false);

        List<Message> unreadMessages = Arrays.asList(m1, m2);
        chat.setMessages(unreadMessages);
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));

        messageService.markAllMessagesAsRead(SENDER_USERNAME, chatId);

        assertTrue(m1.isRead());
        assertTrue(m2.isRead());
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(messageRepository).saveAllAndFlush(captor.capture());
        List<Message> savedMessages = captor.getValue();
        assertEquals(2, savedMessages.size());
    }

    @Test
    void markAllMessagesAsRead_NoUnreadMessages_ShouldNotCallSaveAllAndFlush() {
        when(authentication.getName()).thenReturn(SENDER_USERNAME);
        Message readMsg = new Message();
        readMsg.setId(UUID.randomUUID());
        readMsg.setSender(receiver);
        readMsg.setRecipient(sender);
        readMsg.setRead(true);
        chat.setMessages(List.of(readMsg));
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));

        messageService.markAllMessagesAsRead(SENDER_USERNAME, chatId);
        verify(messageRepository, never()).saveAllAndFlush(any());
    }


    @Test
    void markAllMessagesAsRead_Unauthorized_ShouldThrowAccessDenied() {
        when(authentication.getName()).thenReturn("otherUser");
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                messageService.markAllMessagesAsRead(SENDER_USERNAME, chatId));
        assertEquals("You can only mark your own incoming messages as read", exception.getMessage());
    }

    @Test
    void markAllMessagesAsRead_ChatNotFound_ShouldThrowResourceNotFound() {
        when(chatRepository.findById(chatId)).thenReturn(Optional.empty());
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                messageService.markAllMessagesAsRead(SENDER_USERNAME, chatId));
        assertTrue(exception.getMessage().contains("Chat"));
    }

    @Test
    void markAllMessagesAsRead_NotParticipant_ShouldThrowAccessDenied() {
        Chat chatNotParticipant = new Chat();
        chatNotParticipant.setId(chatId);
        User nonParticipant = new User();
        nonParticipant.setUsername("nonParticipant");
        chatNotParticipant.setUser1(nonParticipant);
        chatNotParticipant.setUser2(receiver);
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chatNotParticipant));
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                messageService.markAllMessagesAsRead(SENDER_USERNAME, chatId));
        assertEquals("You can only read messages in your own chat", exception.getMessage());
    }

    @Test
    void updateMessage_Success() {
        when(authentication.getName()).thenReturn(SENDER_USERNAME);
        Message existingMsg = new Message();
        existingMsg.setId(messageId);
        existingMsg.setSender(sender);
        existingMsg.setRecipient(receiver);
        existingMsg.setEncryptedContent("oldEnc");
        existingMsg.setPlainContent("Old text");
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(existingMsg));
        when(encryptionService.encrypt(updateDto.getNewMessageText())).thenReturn("encrypted_" + updateDto.getNewMessageText());
        when(messageRepository.saveAndFlush(existingMsg)).thenReturn(existingMsg);
        when(messageMapper.toDto(existingMsg)).thenReturn(responseDto);

        MessageResponseDto result = messageService.updateMessage(SENDER_USERNAME, messageId, updateDto);
        assertNotNull(result);
        assertEquals(SENDER_USERNAME, result.getSenderUsername());
        verify(messageRepository).saveAndFlush(existingMsg);
    }

    @Test
    void updateMessage_MessageNotFound_ShouldThrowResourceNotFound() {
        when(authentication.getName()).thenReturn(SENDER_USERNAME);
        when(messageRepository.findById(messageId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () ->
                messageService.updateMessage(SENDER_USERNAME, messageId, updateDto));
    }

    @Test
    void updateMessage_NotOwner_ShouldThrowAccessDenied() {
        when(authentication.getName()).thenReturn(SENDER_USERNAME);
        Message existingMsg = new Message();
        existingMsg.setId(messageId);
        User otherSender = new User();
        otherSender.setUsername("otherUser");
        existingMsg.setSender(otherSender);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(existingMsg));
        assertThrows(AccessDeniedException.class, () ->
                messageService.updateMessage(SENDER_USERNAME, messageId, updateDto));
    }

    @Test
    void updateMessage_AuthMismatch_ShouldThrowAccessDenied() {
        when(authentication.getName()).thenReturn("differentUser");
        assertThrows(AccessDeniedException.class, () ->
                messageService.updateMessage(SENDER_USERNAME, messageId, updateDto));
    }

    @Test
    void deleteMessage_Success() {
        when(authentication.getName()).thenReturn(SENDER_USERNAME);
        Message msg = new Message();
        msg.setId(messageId);
        msg.setSender(sender);
        msg.setRecipient(receiver);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(msg));
        messageService.deleteMessage(SENDER_USERNAME, messageId);
        verify(messageRepository).delete(msg);
    }

    @Test
    void deleteMessage_MessageNotFound_ShouldThrowResourceNotFound() {
        when(messageRepository.findById(messageId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () ->
                messageService.deleteMessage(SENDER_USERNAME, messageId));
    }

    @Test
    void deleteMessage_NotOwner_ShouldThrowAccessDenied() {
        when(authentication.getName()).thenReturn(SENDER_USERNAME);
        Message msg = new Message();
        msg.setId(messageId);
        User otherSender = new User();
        otherSender.setUsername("otherUser");
        msg.setSender(otherSender);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(msg));
        assertThrows(AccessDeniedException.class, () ->
                messageService.deleteMessage(SENDER_USERNAME, messageId));
    }

    @Test
    void deleteMessage_AuthMismatch_ShouldThrowAccessDenied() {
        when(authentication.getName()).thenReturn("differentUser");
        assertThrows(AccessDeniedException.class, () ->
                messageService.deleteMessage(SENDER_USERNAME, messageId));
    }
}
