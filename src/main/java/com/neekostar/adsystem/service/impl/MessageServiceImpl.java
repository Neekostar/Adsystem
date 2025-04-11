package com.neekostar.adsystem.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
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
import com.neekostar.adsystem.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class MessageServiceImpl implements MessageService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;
    private final MessageMapper messageMapper;

    @Autowired
    public MessageServiceImpl(ChatRepository chatRepository,
                              MessageRepository messageRepository,
                              UserRepository userRepository,
                              EncryptionService encryptionService,
                              MessageMapper messageMapper) {
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
        this.messageMapper = messageMapper;
    }

    @Override
    @Transactional
    public MessageResponseDto sendMessage(UUID chatId, String senderUsername, MessageCreateDto dto) {
        log.info("Sending message from '{}' to chat '{}'", senderUsername, chatId);
        String authenticatedUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!authenticatedUsername.equals(senderUsername)) {
            log.error("User '{}' attempted to send a message on behalf of user '{}'", authenticatedUsername, senderUsername);
            throw new AccessDeniedException("You can only send messages on your own behalf");
        }

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> {
                    log.error("Chat not found: {}", chatId);
                    return new ResourceNotFoundException("Chat", "id", chatId.toString());
                });

        if (!isParticipant(chat, senderUsername)) {
            log.error("User '{}' is not a participant of chat '{}'", senderUsername, chatId);
            throw new AccessDeniedException("You are not a participant of this chat");
        }

        User sender = userRepository.findUserByUsername(senderUsername)
                .orElseThrow(() -> {
                    log.error("Sender not found: {}", senderUsername);
                    return new ResourceNotFoundException("User", "username", senderUsername);
                });
        User recipient = chat.getUser1().equals(sender) ? chat.getUser2() : chat.getUser1();

        String encryptedMessage = encryptionService.encrypt(dto.getMessageText());
        log.debug("Message text encrypted successfully for sender '{}'", senderUsername);

        Message message = new Message();
        message.setChat(chat);
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setEncryptedContent(encryptedMessage);
        message.setRead(false);

        messageRepository.saveAndFlush(message);
        log.info("Message saved with id '{}' in chat '{}'", message.getId(), chatId);

        message.setPlainContent(dto.getMessageText());
        autoMarkIncomingAsRead(chat, sender);

        return messageMapper.toDto(message);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponseDto> getMessagesForChat(String username, UUID chatId) {
        log.info("Fetching messages for chat '{}' for user '{}'", chatId, username);
        String authenticatedUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!authenticatedUsername.equals(username)) {
            log.error("User '{}' attempted to access messages of user '{}'", authenticatedUsername, username);
            throw new AccessDeniedException("You can only access your own messages");
        }

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> {
                    log.error("Chat not found: {}", chatId);
                    return new ResourceNotFoundException("Chat", "id", chatId.toString());
                });
        if (!isParticipant(chat, username)) {
            log.error("User '{}' is not a participant of chat '{}'", username, chatId);
            throw new AccessDeniedException("You can only access your own chats");
        }

        List<Message> messages = messageRepository.findByChatOrderByCreatedAtAsc(chat);
        log.info("Found {} messages in chat '{}'", messages.size(), chatId);

        return messages.stream()
                .map(message -> {
                    try {
                        String decrypted = encryptionService.decrypt(message.getEncryptedContent());
                        message.setPlainContent(decrypted);
                    } catch (Exception e) {
                        log.error("Failed to decrypt message with id '{}': {}", message.getId(), e.getMessage());
                        message.setPlainContent(null);
                    }
                    return messageMapper.toDto(message);
                })
                .collect(Collectors.toList());
    }

    @Override
    public void markMessageAsRead(String username, UUID messageId) {
        log.info("Marking message '{}' as read for user '{}'", messageId, username);
        String authenticatedUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!authenticatedUsername.equals(username)) {
            log.error("User '{}' attempted to mark message '{}' as read for user '{}'", authenticatedUsername, messageId, username);
            throw new AccessDeniedException("You can only mark your own messages as read");
        }
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> {
                    log.error("Message not found: {}", messageId);
                    return new ResourceNotFoundException("Message", "id", messageId.toString());
                });
        if (!message.getRecipient().getUsername().equals(username)) {
            log.error("User '{}' attempted to mark message '{}' as read which is not addressed to them", username, messageId);
            throw new AccessDeniedException("You can only mark your own messages as read");
        }
        message.markAsRead();
        messageRepository.saveAndFlush(message);
        log.info("Message '{}' marked as read successfully", messageId);
    }

    @Override
    @Transactional
    public void markAllMessagesAsRead(String username, UUID chatId) {
        log.info("Marking all unread messages as read in chat '{}' for user '{}'", chatId, username);

        String authenticatedUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!authenticatedUsername.equals(username)) {
            log.error("User '{}' attempted to mark messages as read on behalf of '{}'", authenticatedUsername, username);
            throw new AccessDeniedException("You can only mark your own incoming messages as read");
        }

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> {
                    log.error("Chat not found: {}", chatId);
                    return new ResourceNotFoundException("Chat", "id", chatId.toString());
                });

        if (!isParticipant(chat, username)) {
            log.error("User '{}' is not a participant of chat '{}'", username, chatId);
            throw new AccessDeniedException("You can only read messages in your own chat");
        }

        List<Message> unreadMessages = chat.getMessages().stream()
                .filter(m -> !m.isRead() && m.getRecipient().getUsername().equals(username))
                .collect(Collectors.toList());

        if (unreadMessages.isEmpty()) {
            log.info("No unread messages found in chat '{}' for user '{}'", chatId, username);
            return;
        }

        unreadMessages.forEach(Message::markAsRead);
        messageRepository.saveAllAndFlush(unreadMessages);
        log.info("{} messages were marked as read in chat '{}' for user '{}'", unreadMessages.size(), chatId, username);
    }

    @Override
    @Transactional
    public MessageResponseDto updateMessage(String username, UUID messageId, MessageUpdateDto updateDto) {
        log.info("Updating message '{}' for user '{}'", messageId, username);
        String authenticatedUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!authenticatedUsername.equals(username)) {
            log.error("User '{}' attempted to update message '{}' on behalf of user '{}'", authenticatedUsername, messageId, username);
            throw new AccessDeniedException("You can only update your own messages");
        }
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> {
                    log.error("Message not found: {}", messageId);
                    return new ResourceNotFoundException("Message", "id", messageId.toString());
                });
        if (!message.getSender().getUsername().equals(username)) {
            log.error("User '{}' attempted to update message '{}' which they do not own", username, messageId);
            throw new AccessDeniedException("You can only update your own messages");
        }

        String encryptedMessage = encryptionService.encrypt(updateDto.getNewMessageText());
        message.setEncryptedContent(encryptedMessage);
        message.setPlainContent(updateDto.getNewMessageText());
        messageRepository.saveAndFlush(message);
        log.info("Message '{}' updated successfully", messageId);

        return messageMapper.toDto(message);
    }

    @Override
    @Transactional
    public void deleteMessage(String username, UUID messageId) {
        log.info("Deleting message '{}' for user '{}'", messageId, username);
        String authenticatedUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!authenticatedUsername.equals(username)) {
            log.error("User '{}' attempted to delete message '{}' on behalf of user '{}'", authenticatedUsername, messageId, username);
            throw new AccessDeniedException("You can only delete your own messages");
        }
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> {
                    log.error("Message not found: {}", messageId);
                    return new ResourceNotFoundException("Message", "id", messageId.toString());
                });
        if (!message.getSender().getUsername().equals(username)) {
            log.error("User '{}' attempted to delete message '{}' which they do not own", username, messageId);
            throw new AccessDeniedException("You can only delete your own messages");
        }
        messageRepository.delete(message);
        log.info("Message '{}' deleted successfully", messageId);
    }

    private boolean isParticipant(@NotNull Chat chat, String username) {
        boolean participant = chat.getUser1().getUsername().equals(username) || chat.getUser2().getUsername().equals(username);
        log.debug("User '{}' is {}a participant of chat '{}'", username, participant ? "" : "not ", chat.getId());
        return participant;
    }

    private void autoMarkIncomingAsRead(@NotNull Chat chat, User userJustSentMessage) {
        List<Message> unreadForThisUser = chat.getMessages().stream()
                .filter(m -> !m.isRead() && m.getRecipient().equals(userJustSentMessage))
                .collect(Collectors.toList());

        if (!unreadForThisUser.isEmpty()) {
            log.info("Auto marking {} incoming messages as read for user '{}'", unreadForThisUser.size(), userJustSentMessage.getUsername());
            unreadForThisUser.forEach(Message::markAsRead);
            messageRepository.saveAllAndFlush(unreadForThisUser);
        } else {
            log.debug("No incoming unread messages to mark as read for user '{}' in chat '{}'", userJustSentMessage.getUsername(), chat.getId());
        }
    }
}
