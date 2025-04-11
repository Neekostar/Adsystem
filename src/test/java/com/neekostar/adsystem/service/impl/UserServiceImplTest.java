package com.neekostar.adsystem.service.impl;

import com.neekostar.adsystem.dto.UserResponseDto;
import com.neekostar.adsystem.dto.UserUpdateDto;
import com.neekostar.adsystem.exception.AccessDeniedException;
import com.neekostar.adsystem.exception.ResourceNotFoundException;
import com.neekostar.adsystem.mapper.UserMapper;
import com.neekostar.adsystem.model.User;
import com.neekostar.adsystem.repository.UserRepository;
import com.neekostar.adsystem.service.MinioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UserServiceImplTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private Authentication authentication;

    @Mock
    private MinioService minioService;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserResponseDto testUserResponseDto;
    private UserUpdateDto testUserUpdateDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testUser");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setEmail("test@example.com");
        testUser.setAvatarUrl("https://example.com/avatar.jpg");
        testUser.setRating(3.5f);

        testUserResponseDto = new UserResponseDto(
                "testUser", "test@example.com", "John", "Doe", "https://example.com/avatar.jpg", 3.5f, "USER"
        );

        testUserUpdateDto = new UserUpdateDto();
        testUserUpdateDto.setFirstName("Jane");
        testUserUpdateDto.setLastName("Smith");
        testUserUpdateDto.setEmail("new@example.com");
        testUserUpdateDto.setAvatarUrl("https://example.com/new-avatar.jpg");

        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.getName()).thenReturn("testUser");
    }

    @Test
    void getUserByUsername_WithExistingUsername_ShouldReturnUserResponseDto() {
        when(userRepository.findUserByUsername("testUser")).thenReturn(Optional.of(testUser));
        when(userMapper.toDto(testUser)).thenReturn(testUserResponseDto);

        UserResponseDto result = userService.getUserByUsername("testUser");

        assertNotNull(result);
        assertEquals("testUser", result.getUsername());
        verify(userRepository).findUserByUsername("testUser");
        verify(userMapper).toDto(testUser);
    }

    @Test
    void getUserByUsername_NonExisting_ShouldThrowResourceNotFoundException() {
        when(userRepository.findUserByUsername("unknown"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserByUsername("unknown"));
        verify(userRepository).findUserByUsername("unknown");
        verifyNoMoreInteractions(userMapper);
    }

    @Test
    void getAllUsers_WithResults_ShouldReturnPageOfUserResponseDto() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(testUser), pageable, 1);
        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(userMapper.toDto(testUser)).thenReturn(testUserResponseDto);

        Page<UserResponseDto> result = userService.getAllUsers(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("testUser", result.getContent().get(0).getUsername());
        verify(userRepository).findAll(pageable);
        verify(userMapper).toDto(testUser);
    }

    @Test
    void getAllUsers_Empty_ShouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(userRepository.findAll(pageable)).thenReturn(emptyPage);

        Page<UserResponseDto> result = userService.getAllUsers(pageable);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository).findAll(pageable);
        verifyNoMoreInteractions(userMapper);
    }

    @Test
    void updateUser_ValidData_ShouldReturnUpdatedUserResponseDto() {
        when(userRepository.findUserByUsername("testUser"))
                .thenReturn(Optional.of(testUser));
        when(userRepository.existsUserByEmail("new@example.com"))
                .thenReturn(false);
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userMapper.toDto(testUser)).thenReturn(testUserResponseDto);

        UserResponseDto result = userService.updateUser("testUser", testUserUpdateDto);

        assertNotNull(result);
        assertEquals("testUser", result.getUsername());
        assertEquals("new@example.com", testUser.getEmail());
        assertEquals("Jane", testUser.getFirstName());
        assertEquals("Smith", testUser.getLastName());
        assertEquals("https://example.com/new-avatar.jpg", testUser.getAvatarUrl());

        verify(userRepository).save(testUser);
        verify(userMapper).toDto(testUser);
    }

    @Test
    void updateUser_NonExistingUsername_ShouldThrowResourceNotFoundException() {
        when(userRepository.findUserByUsername("unknown"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.updateUser("unknown", testUserUpdateDto));
        verify(userRepository).findUserByUsername("unknown");
    }

    @Test
    void updateUser_UnauthorizedUser_ShouldThrowAccessDeniedException() {
        when(authentication.getName()).thenReturn("anotherUser");
        when(userRepository.findUserByUsername("testUser"))
                .thenReturn(Optional.of(testUser));

        assertThrows(AccessDeniedException.class,
                () -> userService.updateUser("testUser", testUserUpdateDto));
        verify(userRepository).findUserByUsername("testUser");
    }

    @Test
    void updateUser_ExistingEmail_ShouldThrowIllegalArgumentException() {
        when(userRepository.findUserByUsername("testUser"))
                .thenReturn(Optional.of(testUser));
        when(userRepository.existsUserByEmail("new@example.com"))
                .thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> userService.updateUser("testUser", testUserUpdateDto));
        verify(userRepository).findUserByUsername("testUser");
        verify(userRepository).existsUserByEmail("new@example.com");
    }

    @Test
    void updateUser_NoFieldsChanged_ShouldReturnSameUser() {
        UserUpdateDto emptyUpdateDto = new UserUpdateDto();
        when(userRepository.findUserByUsername("testUser"))
                .thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userMapper.toDto(testUser)).thenReturn(testUserResponseDto);

        UserResponseDto result = userService.updateUser("testUser", emptyUpdateDto);

        assertNotNull(result);
        assertEquals("testUser", result.getUsername());
        assertEquals("test@example.com", testUser.getEmail());  // не поменялось
        verify(userRepository).save(testUser);
        verify(userMapper).toDto(testUser);
    }

    @Test
    void deleteUser_ValidUsername_ShouldDeleteUser() {
        when(userRepository.findUserByUsername("testUser"))
                .thenReturn(Optional.of(testUser));

        userService.deleteUser("testUser");

        verify(userRepository).delete(testUser);
    }

    @Test
    void deleteUser_NonExistingUsername_ShouldThrowResourceNotFoundException() {
        when(userRepository.findUserByUsername("unknown"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.deleteUser("unknown"));
        verify(userRepository).findUserByUsername("unknown");
    }

    @Test
    void deleteUser_UnauthorizedUser_ShouldThrowAccessDeniedException() {
        when(authentication.getName()).thenReturn("anotherUser");
        when(userRepository.findUserByUsername("testUser"))
                .thenReturn(Optional.of(testUser));

        assertThrows(AccessDeniedException.class,
                () -> userService.deleteUser("testUser"));
        verify(userRepository).findUserByUsername("testUser");
    }

    @Test
    void uploadUserAvatar_SuccessWithOldAvatar_ShouldRemoveOldAndUploadNew() {
        when(userRepository.findUserByUsername("testUser"))
                .thenReturn(Optional.of(testUser));
        testUser.setAvatarUrl("http://example.com/old-avatar.jpg");
        when(minioService.resolveObjectNameFromUrl("http://example.com/old-avatar.jpg"))
                .thenReturn("old-avatar.jpg");

        MultipartFile mockFile = mock(MultipartFile.class);
        when(minioService.uploadFile(mockFile, "avatars"))
                .thenReturn("http://example.com/new-avatar.jpg");

        when(userRepository.saveAndFlush(testUser)).thenReturn(testUser);
        when(userMapper.toDto(testUser)).thenReturn(testUserResponseDto);

        UserResponseDto result = userService.uploadUserAvatar("testUser", mockFile);

        verify(minioService).removeFile("old-avatar.jpg");
        verify(minioService).uploadFile(mockFile, "avatars");
        verify(userRepository).saveAndFlush(testUser);
        assertEquals("http://example.com/new-avatar.jpg", testUser.getAvatarUrl());
        assertEquals("testUser", result.getUsername());
    }

    @Test
    void uploadUserAvatar_SuccessNoOldAvatar_ShouldJustUpload() {
        when(userRepository.findUserByUsername("testUser"))
                .thenReturn(Optional.of(testUser));
        testUser.setAvatarUrl(null);

        MultipartFile mockFile = mock(MultipartFile.class);
        when(minioService.uploadFile(mockFile, "avatars"))
                .thenReturn("http://example.com/new-avatar2.jpg");

        when(userRepository.saveAndFlush(testUser)).thenReturn(testUser);
        when(userMapper.toDto(testUser)).thenReturn(testUserResponseDto);

        UserResponseDto result = userService.uploadUserAvatar("testUser", mockFile);

        verify(minioService, never()).removeFile(anyString());
        verify(minioService).uploadFile(mockFile, "avatars");
        verify(userRepository).saveAndFlush(testUser);
        assertEquals("http://example.com/new-avatar2.jpg", testUser.getAvatarUrl());
        assertEquals("testUser", result.getUsername());
    }

    @Test
    void uploadUserAvatar_UserNotFound_ShouldThrowResourceNotFoundException() {
        when(userRepository.findUserByUsername("unknown"))
                .thenReturn(Optional.empty());

        MultipartFile mockFile = mock(MultipartFile.class);

        assertThrows(ResourceNotFoundException.class,
                () -> userService.uploadUserAvatar("unknown", mockFile));
    }

    @Test
    void uploadUserAvatar_Unauthorized_ShouldThrowAccessDeniedException() {
        when(authentication.getName()).thenReturn("anotherUser");
        when(userRepository.findUserByUsername("testUser"))
                .thenReturn(Optional.of(testUser));

        MultipartFile mockFile = mock(MultipartFile.class);

        assertThrows(AccessDeniedException.class,
                () -> userService.uploadUserAvatar("testUser", mockFile));
    }

    @Test
    void removeUserAvatar_SuccessWithOldAvatar_ShouldRemoveIt() {
        when(userRepository.findUserByUsername("testUser"))
                .thenReturn(Optional.of(testUser));
        testUser.setAvatarUrl("http://example.com/old-avatar.jpg");

        when(minioService.resolveObjectNameFromUrl("http://example.com/old-avatar.jpg"))
                .thenReturn("old-avatar.jpg");

        userService.removeUserAvatar("testUser");

        verify(minioService).removeFile("old-avatar.jpg");
        verify(userRepository).saveAndFlush(testUser);
        assertNull(testUser.getAvatarUrl());
    }

    @Test
    void removeUserAvatar_SuccessNoOldAvatar_ShouldSkipRemoval() {
        when(userRepository.findUserByUsername("testUser"))
                .thenReturn(Optional.of(testUser));
        testUser.setAvatarUrl(null);

        userService.removeUserAvatar("testUser");

        verify(minioService, never()).removeFile(anyString());
        verify(userRepository, never()).saveAndFlush(any());
        assertNull(testUser.getAvatarUrl());
    }

    @Test
    void removeUserAvatar_UserNotFound_ShouldThrowResourceNotFoundException() {
        when(userRepository.findUserByUsername("unknown"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.removeUserAvatar("unknown"));
    }

    @Test
    void removeUserAvatar_Unauthorized_ShouldThrowAccessDeniedException() {
        when(authentication.getName()).thenReturn("anotherUser");
        when(userRepository.findUserByUsername("testUser"))
                .thenReturn(Optional.of(testUser));

        assertThrows(AccessDeniedException.class,
                () -> userService.removeUserAvatar("testUser"));
    }
}
