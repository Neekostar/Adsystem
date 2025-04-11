package com.neekostar.adsystem.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import com.neekostar.adsystem.dto.ChatResponseDto;
import com.neekostar.adsystem.dto.ChatUnreadDto;
import com.neekostar.adsystem.dto.UnreadChatsInfoDto;
import com.neekostar.adsystem.exception.AccessDeniedException;
import com.neekostar.adsystem.exception.ResourceNotFoundException;
import com.neekostar.adsystem.mapper.ChatMapper;
import com.neekostar.adsystem.model.Chat;
import com.neekostar.adsystem.model.User;
import com.neekostar.adsystem.repository.ChatRepository;
import com.neekostar.adsystem.repository.UserRepository;
import com.neekostar.adsystem.service.ChatService;
import com.neekostar.adsystem.service.EncryptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final ChatMapper chatMapper;
    private final EncryptionService encryptionService;

    @Autowired
    public ChatServiceImpl(ChatRepository chatRepository,
                           UserRepository userRepository,
                           ChatMapper chatMapper,
                           EncryptionService encryptionService) {
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.chatMapper = chatMapper;
        this.encryptionService = encryptionService;
    }

    @Override
    @Transactional
    public Chat getOrCreateChat(String initiatorUsername, String otherUsername) {
        log.info("Getting or creating chat between {} and {}", initiatorUsername, otherUsername);
        if (initiatorUsername.equals(otherUsername)) {
            log.error("Users cannot create a chat with themselves");
            throw new IllegalArgumentException("Users cannot create a chat with themselves");
        }

        String authenticatedUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!authenticatedUsername.equals(initiatorUsername)) {
            log.error("User {} attempted to create a chat on behalf of user {}", authenticatedUsername, initiatorUsername);
            throw new AccessDeniedException("You can only create a chat on your own behalf");
        }

        User initiator = userRepository.findUserByUsername(initiatorUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", initiatorUsername));
        User other = userRepository.findUserByUsername(otherUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", otherUsername));

        Chat existing = chatRepository.findByUser1AndUser2(initiator, other)
                .orElseGet(() -> chatRepository.findByUser2AndUser1(initiator, other).orElse(null));

        if (existing != null) {
            log.info("Chat already exists between {} and {}", initiatorUsername, otherUsername);
            return existing;
        }

        Chat newChat = new Chat();
        newChat.setUser1(initiator);
        newChat.setUser2(other);
        Chat savedChat = chatRepository.saveAndFlush(newChat);
        log.info("New chat created between {} and {}", initiatorUsername, otherUsername);

        return savedChat;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatResponseDto> getAllChatsForUser(String username) {
        log.info("Getting all chats for user {}", username);
        String authenticatedUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!authenticatedUsername.equals(username)) {
            log.error("User {} attempted to access chats of user {}", authenticatedUsername, username);
            throw new AccessDeniedException("You can only access your own chats");
        }
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        List<Chat> chats = chatRepository.findByUser1OrUser2(user, user);
        for (Chat chat : chats) {
            chat.calculateUnreadMessagesFor(username);
            chat.getMessages().forEach(message -> {
                try {
                    String decrypted = encryptionService.decrypt(message.getEncryptedContent());
                    message.setPlainContent(decrypted);
                } catch (Exception e) {
                    log.error("Failed to decrypt message with id {}: {}", message.getId(), e.getMessage());
                    message.setPlainContent(null);
                }
            });
        }

        return chats.stream()
                .map(chatMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ChatResponseDto getChatDetails(String username, UUID chatId) {
        log.info("Getting chat details for chat {} for user {}", chatId, username);
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat", "id", chatId.toString()));

        String authenticatedUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!authenticatedUsername.equals(username)) {
            log.error("User {} attempted to access chat {} of user {}", authenticatedUsername, chatId, username);
            throw new AccessDeniedException("You can only access your own chats");
        }
        if (!chat.getUser1().getUsername().equals(username) && !chat.getUser2().getUsername().equals(username)) {
            log.error("Chat {} does not belong to user {}", chatId, username);
            throw new AccessDeniedException("Chat does not belong to the current user");
        }

        chat.calculateUnreadMessagesFor(username);

        chat.getMessages().forEach(message -> {
            try {
                String decrypted = encryptionService.decrypt(message.getEncryptedContent());
                message.setPlainContent(decrypted);
            } catch (Exception e) {
                log.error("Failed to decrypt message with id {}: {}", message.getId(), e.getMessage());
                message.setPlainContent(null);
            }
        });

        return chatMapper.toDto(chat);
    }

    @Override
    @Transactional(readOnly = true)
    public int getUnreadChatsCount(String username) {
        log.info("Getting unread chats count for user {}", username);
        String authenticatedUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!authenticatedUsername.equals(username)) {
            log.error("User {} attempted to access unread chats count of user {}", authenticatedUsername, username);
            throw new AccessDeniedException("You can only access your own chats");
        }
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        List<Chat> chats = chatRepository.findByUser1OrUser2(user, user);
        int unreadChats = 0;
        for (Chat chat : chats) {
            chat.calculateUnreadMessagesFor(username);
            if (chat.getUnreadMessagesCount() > 0) {
                unreadChats++;
            }
        }
        return unreadChats;
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadChatsInfoDto getUnreadInfo(String username) {
        log.info("Getting unread info for user {}", username);

        String authenticatedUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!authenticatedUsername.equals(username)) {
            log.error("User {} attempted to get unread info for {}", authenticatedUsername, username);
            throw new AccessDeniedException("You can only view your own unread info");
        }

        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        List<Chat> chats = chatRepository.findByUser1OrUser2(user, user);

        List<ChatUnreadDto> chatUnreadList = new ArrayList<>();
        int totalUnread = 0;

        for (Chat chat : chats) {
            chat.calculateUnreadMessagesFor(username);
            int unreadMessages = chat.getUnreadMessagesCount();

            ChatUnreadDto chatUnreadDto = new ChatUnreadDto();
            chatUnreadDto.setChatId(chat.getId());
            chatUnreadDto.setUnreadCount(unreadMessages);

            chatUnreadList.add(chatUnreadDto);

            totalUnread += unreadMessages;
        }

        UnreadChatsInfoDto result = new UnreadChatsInfoDto();
        result.setTotalUnread(totalUnread);
        result.setChats(chatUnreadList);

        return result;
    }
}
