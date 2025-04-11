package com.neekostar.adsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neekostar.adsystem.dto.UserResponseDto;
import com.neekostar.adsystem.dto.UserUpdateDto;
import com.neekostar.adsystem.exception.AccessDeniedException;
import com.neekostar.adsystem.exception.GlobalExceptionHandler;
import com.neekostar.adsystem.exception.ResourceNotFoundException;
import com.neekostar.adsystem.service.UserService;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private static final String API_USERS = "/api/users";
    private static final String APPLICATION_JSON = "application/json";
    private static final String SAMPLE_USERNAME = "testuser";
    private static final String SAMPLE_EMAIL = "testuser@mail.com";
    private static final String SAMPLE_FIRST_NAME = "Test";
    private static final String SAMPLE_LAST_NAME = "User";
    private static final String SAMPLE_AVATAR_URL = "http://example.com/avatar.jpg";
    private static final String UPDATED_FIRST_NAME = "UpdatedFirst";
    private static final String UPDATED_LAST_NAME = "UpdatedLast";
    private static final String UPDATED_EMAIL = "updated@mail.com";
    private static final String UPDATED_AVATAR_URL = "http://example.com/newavatar.jpg";

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler(messageSource))
                .build();
    }

    @Test
    @DisplayName("GET /api/users - get all users (success)")
    void getAllUsers_Success() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserResponseDto> page = new PageImpl<>(List.of(buildUserResponseDto(), buildUserResponseDto()), pageable, 2);
        when(userService.getAllUsers(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get(API_USERS)
                        .with(csrf())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/users/{username} - get user by username")
    void getUserByUsername_Success() throws Exception {
        UserResponseDto dto = buildUserResponseDto();
        when(userService.getUserByUsername(SAMPLE_USERNAME)).thenReturn(dto);

        mockMvc.perform(get(API_USERS + "/{username}", SAMPLE_USERNAME).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(SAMPLE_USERNAME)))
                .andExpect(jsonPath("$.email", is(SAMPLE_EMAIL)));
    }

    @Test
    @DisplayName("GET /api/users/{username} - user not found")
    void getUserByUsername_NotFound() throws Exception {
        when(userService.getUserByUsername(SAMPLE_USERNAME))
                .thenThrow(new ResourceNotFoundException("User", "username", SAMPLE_USERNAME));

        mockMvc.perform(get(API_USERS + "/{username}", SAMPLE_USERNAME)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("PUT /api/users/{username} - update user")
    void updateUser_Success() throws Exception {
        UserUpdateDto updateDto = buildUserUpdateDto();
        UserResponseDto updatedDto = buildUserResponseDto();
        updatedDto.setFirstName(UPDATED_FIRST_NAME);
        updatedDto.setLastName(UPDATED_LAST_NAME);
        updatedDto.setEmail(UPDATED_EMAIL);
        updatedDto.setAvatarUrl(UPDATED_AVATAR_URL);

        when(userService.updateUser(eq(SAMPLE_USERNAME), any(UserUpdateDto.class))).thenReturn(updatedDto);

        mockMvc.perform(put(API_USERS + "/{username}", SAMPLE_USERNAME)
                        .contentType(APPLICATION_JSON)
                        .with(csrf())
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is(UPDATED_FIRST_NAME)))
                .andExpect(jsonPath("$.lastName", is(UPDATED_LAST_NAME)))
                .andExpect(jsonPath("$.email", is(UPDATED_EMAIL)))
                .andExpect(jsonPath("$.avatarUrl", is(UPDATED_AVATAR_URL)));
    }

    @Test
    @DisplayName("PUT /api/users/{username} - update user not found")
    void updateUser_NotFound() throws Exception {
        UserUpdateDto updateDto = buildUserUpdateDto();
        when(userService.updateUser(eq(SAMPLE_USERNAME), any(UserUpdateDto.class)))
                .thenThrow(new ResourceNotFoundException("User", "username", SAMPLE_USERNAME));

        mockMvc.perform(put(API_USERS + "/{username}", SAMPLE_USERNAME)
                        .contentType(APPLICATION_JSON)
                        .with(csrf())
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/users/{username} - update user access denied")
    void updateUser_AccessDenied() throws Exception {
        UserUpdateDto updateDto = buildUserUpdateDto();
        when(userService.updateUser(eq(SAMPLE_USERNAME), any(UserUpdateDto.class)))
                .thenThrow(new AccessDeniedException("You can only update your own profile"));

        mockMvc.perform(put(API_USERS + "/{username}", SAMPLE_USERNAME)
                        .contentType(APPLICATION_JSON)
                        .with(csrf())
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/users/{username} - delete user")
    void deleteUser_Success() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(SAMPLE_USERNAME, "password")
        );
        doNothing().when(userService).deleteUser(SAMPLE_USERNAME);

        mockMvc.perform(delete(API_USERS + "/{username}", SAMPLE_USERNAME)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(SAMPLE_USERNAME);
    }

    @Test
    @DisplayName("DELETE /api/users/{username} - delete user not found")
    void deleteUser_NotFound() throws Exception {
        doThrow(new ResourceNotFoundException("User", "username", SAMPLE_USERNAME))
                .when(userService).deleteUser(SAMPLE_USERNAME);
        mockMvc.perform(delete(API_USERS + "/{username}", SAMPLE_USERNAME)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/users/{username} - delete user access denied")
    void deleteUser_AccessDenied() throws Exception {
        doThrow(new AccessDeniedException("You can only delete your own profile"))
                .when(userService).deleteUser(SAMPLE_USERNAME);
        mockMvc.perform(delete(API_USERS + "/{username}", SAMPLE_USERNAME)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/users/{username}/avatar - upload avatar")
    void uploadUserAvatar_Success() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(SAMPLE_USERNAME, "password")
        );

        byte[] content = "dummy image content".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", content);

        UserResponseDto dto = buildUserResponseDto();
        when(userService.uploadUserAvatar(eq(SAMPLE_USERNAME), eq(file))).thenReturn(dto);

        mockMvc.perform(multipart(API_USERS + "/{username}/avatar", SAMPLE_USERNAME)
                        .file(file)
                        .with(csrf())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(SAMPLE_USERNAME)))
                .andExpect(jsonPath("$.avatarUrl", is(SAMPLE_AVATAR_URL)));
    }

    @Test
    @DisplayName("POST /api/users/{username}/avatar - upload avatar access denied")
    void uploadUserAvatar_AccessDenied() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anotherUser", "password")
        );
        byte[] content = "dummy image content".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", content);

        when(userService.uploadUserAvatar(eq(SAMPLE_USERNAME), any()))
                .thenThrow(new AccessDeniedException("You can only upload avatar for your own profile"));

        mockMvc.perform(multipart(API_USERS + "/{username}/avatar", SAMPLE_USERNAME)
                        .file(file)
                        .with(csrf())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/users/{username}/avatar - upload avatar user not found")
    void uploadUserAvatar_UserNotFound() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(SAMPLE_USERNAME, "password")
        );
        byte[] content = "dummy image content".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", content);

        when(userService.uploadUserAvatar(eq(SAMPLE_USERNAME), any()))
                .thenThrow(new ResourceNotFoundException("User", "username", SAMPLE_USERNAME));

        mockMvc.perform(multipart(API_USERS + "/{username}/avatar", SAMPLE_USERNAME)
                        .file(file)
                        .with(csrf())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/users/{username}/avatar - remove avatar")
    void removeUserAvatar_Success() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(SAMPLE_USERNAME, "password")
        );
        doNothing().when(userService).removeUserAvatar(SAMPLE_USERNAME);

        mockMvc.perform(delete(API_USERS + "/{username}/avatar", SAMPLE_USERNAME)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(userService).removeUserAvatar(SAMPLE_USERNAME);
    }

    @Test
    @DisplayName("DELETE /api/users/{username}/avatar - remove avatar access denied")
    void removeUserAvatar_AccessDenied() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anotherUser", "password")
        );
        doThrow(new AccessDeniedException("You can only remove your own avatar"))
                .when(userService).removeUserAvatar(SAMPLE_USERNAME);

        mockMvc.perform(delete(API_USERS + "/{username}/avatar", SAMPLE_USERNAME)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/users/{username}/avatar - remove avatar user not found")
    void removeUserAvatar_UserNotFound() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(SAMPLE_USERNAME, "password")
        );
        doThrow(new ResourceNotFoundException("User", "username", SAMPLE_USERNAME))
                .when(userService).removeUserAvatar(SAMPLE_USERNAME);

        mockMvc.perform(delete(API_USERS + "/{username}/avatar", SAMPLE_USERNAME)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Contract(" -> new")
    private @NotNull UserResponseDto buildUserResponseDto() {
        return new UserResponseDto(SAMPLE_USERNAME,
                SAMPLE_EMAIL,
                SAMPLE_FIRST_NAME,
                SAMPLE_LAST_NAME,
                SAMPLE_AVATAR_URL,
                4.5f,
                "USER");
    }

    private @NotNull UserUpdateDto buildUserUpdateDto() {
        UserUpdateDto dto = new UserUpdateDto();
        dto.setFirstName(UPDATED_FIRST_NAME);
        dto.setLastName(UPDATED_LAST_NAME);
        dto.setEmail(UPDATED_EMAIL);
        dto.setAvatarUrl(UPDATED_AVATAR_URL);
        return dto;
    }
}