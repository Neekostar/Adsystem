package com.neekostar.adsystem.service.impl;

import com.neekostar.adsystem.dto.UserResponseDto;
import com.neekostar.adsystem.dto.UserUpdateDto;
import com.neekostar.adsystem.exception.AccessDeniedException;
import com.neekostar.adsystem.exception.ResourceNotFoundException;
import com.neekostar.adsystem.mapper.UserMapper;
import com.neekostar.adsystem.model.User;
import com.neekostar.adsystem.repository.UserRepository;
import com.neekostar.adsystem.service.MinioService;
import com.neekostar.adsystem.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@Transactional
public class UserServiceImpl implements UserService {
    private final MinioService minioService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Autowired
    public UserServiceImpl(MinioService minioService,
                           UserRepository userRepository,
                           UserMapper userMapper) {
        this.minioService = minioService;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "userDetails", key = "#username")
    public UserResponseDto getUserByUsername(String username) {
        log.info("Getting user by username: {}", username);
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found by username: {}", username);
                    return new ResourceNotFoundException("User", "username", username);
                });
        log.info("Found user by username: {}", username);
        return userMapper.toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "allUsers", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<UserResponseDto> getAllUsers(Pageable pageable) {
        log.info("Getting all users");
        return userRepository.findAll(pageable)
                .map(userMapper::toDto);
    }

    @Override
    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = "userDetails", key = "#username"),
                    @CacheEvict(value = "allUsers", allEntries = true)
            }
    )
    public UserResponseDto updateUser(String username, UserUpdateDto userUpdateDto) {
        log.info("Updating user by username: {}", username);
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found for update: {}", username);
                    return new ResourceNotFoundException("User", "username", username);
                });

        String authenticatedUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!authenticatedUsername.equals(username)) {
            log.warn("Access denied: User {} attempted to update profile for {}", authenticatedUsername, username);
            throw new AccessDeniedException("You can only update your own profile");
        }

        if (userUpdateDto.getFirstName() != null) {
            log.info("Updating first name for user: {}", username);
            user.setFirstName(userUpdateDto.getFirstName());
        }

        if (userUpdateDto.getLastName() != null) {
            log.info("Updating last name for user: {}", username);
            user.setLastName(userUpdateDto.getLastName());
        }

        if (userUpdateDto.getEmail() != null) {
            log.info("Updating email for user: {}", username);
            if (userRepository.existsUserByEmail(userUpdateDto.getEmail())) {
                log.error("Email {} is already in use", userUpdateDto.getEmail());
                throw new IllegalArgumentException("User with email " + userUpdateDto.getEmail() + " already exists");
            }
            user.setEmail(userUpdateDto.getEmail());
        }

        if (userUpdateDto.getAvatarUrl() != null) {
            log.info("Updating avatar URL for user: {}", username);
            user.setAvatarUrl(userUpdateDto.getAvatarUrl());
        }

        User savedUser = userRepository.save(user);
        log.info("User updated by username: {}", username);

        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = "userDetails", key = "#username"),
                    @CacheEvict(value = "allUsers", allEntries = true)
            }
    )
    public void deleteUser(String username) {
        log.info("Deleting user by username: {}", username);
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found for deletion: {}", username);
                    return new ResourceNotFoundException("User", "username", username);
                });

        String authenticatedUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!authenticatedUsername.equals(username)) {
            log.warn("Access denied: User {} attempted to delete profile for {}", authenticatedUsername, username);
            throw new AccessDeniedException("You can only delete your own profile");
        }

        userRepository.delete(user);
        log.info("User successfully deleted by username: {}", username);
    }

    @Override
    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = "userDetails", key = "#username"),
                    @CacheEvict(value = "allUsers", allEntries = true)
            }
    )
    public UserResponseDto uploadUserAvatar(String username, MultipartFile file) {
        log.info("Uploading avatar for user: {}", username);

        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found for avatar upload: {}", username);
                    return new ResourceNotFoundException("User", "username", username);
                });

        String authenticatedUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!authenticatedUsername.equals(username)) {
            log.warn("Access denied: User {} attempted to upload avatar for {}", authenticatedUsername, username);
            throw new AccessDeniedException("You can only upload avatar for your own profile");
        }

        if (user.getAvatarUrl() != null) {
            String oldObjectName = minioService.resolveObjectNameFromUrl(user.getAvatarUrl());
            minioService.removeFile(oldObjectName);
            log.info("Old avatar removed for user: {}", username);
        }

        String avatarUrl = minioService.uploadFile(file, "avatars");
        user.setAvatarUrl(avatarUrl);

        User savedUser = userRepository.saveAndFlush(user);
        log.info("Avatar uploaded for user: {}", username);

        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(value = "userDetails", key = "#username"),
                    @CacheEvict(value = "allUsers", allEntries = true)
            }
    )
    public void removeUserAvatar(String username) {
        log.info("Removing avatar for user: {}", username);

        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found for avatar removal: {}", username);
                    return new ResourceNotFoundException("User", "username", username);
                });

        String authenticatedUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!authenticatedUsername.equals(username)) {
            log.warn("Access denied: User {} attempted to remove avatar for {}", authenticatedUsername, username);
            throw new AccessDeniedException("You can only remove avatar for your own profile");
        }

        if (user.getAvatarUrl() != null) {
            String objectName = minioService.resolveObjectNameFromUrl(user.getAvatarUrl());
            minioService.removeFile(objectName);
            user.setAvatarUrl(null);
            userRepository.saveAndFlush(user);
            log.info("Avatar removed for user: {}", username);
        } else {
            log.warn("No avatar found for user: {}", username);
        }
    }
}
