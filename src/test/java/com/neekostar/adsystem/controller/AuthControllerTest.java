package com.neekostar.adsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neekostar.adsystem.dto.UserChangePasswordDto;
import com.neekostar.adsystem.dto.UserLoginDto;
import com.neekostar.adsystem.dto.UserRegistrationDto;
import com.neekostar.adsystem.dto.UserResponseDto;
import com.neekostar.adsystem.exception.GlobalExceptionHandler;
import com.neekostar.adsystem.service.AuthService;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    private static final String API_AUTH = "/api/auth";
    private static final String API_REGISTER = API_AUTH + "/register";
    private static final String API_LOGIN = API_AUTH + "/login";
    private static final String API_LOGOUT = API_AUTH + "/logout";
    private static final String API_CHANGE_PASSWORD = API_AUTH + "/change-password";
    private static final String APPLICATION_JSON = "application/json";

    private static final String SAMPLE_USERNAME = "testuser";
    private static final String SAMPLE_PASSWORD = "password";
    private static final String SAMPLE_WRONG_PASSWORD = "wrongPassword";
    private static final String SAMPLE_EMAIL = "testuser@mail.com";
    private static final String SAMPLE_FIRST_NAME = "Test";
    private static final String SAMPLE_LAST_NAME = "User";

    private static final String OLD_PASSWORD = "oldPass";
    private static final String NEW_PASSWORD = "newPass";

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler(messageSource))
                .build();
    }

    @Test
    @DisplayName("POST /api/auth/register - success")
    void register_Success() throws Exception {
        UserRegistrationDto registrationDto = buildUserRegistrationDto();
        UserResponseDto responseDto = buildUserResponseDto();

        when(authService.register(any(UserRegistrationDto.class))).thenReturn(responseDto);

        mockMvc.perform(post(API_REGISTER)
                        .contentType(APPLICATION_JSON)
                        .with(csrf())
                        .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is(SAMPLE_USERNAME)))
                .andExpect(jsonPath("$.email", is(SAMPLE_EMAIL)));
    }

    @Test
    @DisplayName("POST /api/auth/login - success")
    void login_Success() throws Exception {
        UserLoginDto loginDto = buildUserLoginDto();
        UserResponseDto responseDto = buildUserResponseDto();

        when(authService.login(any(UserLoginDto.class))).thenReturn(responseDto);

        mockMvc.perform(post(API_LOGIN)
                        .contentType(APPLICATION_JSON)
                        .with(csrf())
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(SAMPLE_USERNAME)))
                .andExpect(jsonPath("$.email", is(SAMPLE_EMAIL)));
    }

    @Test
    @DisplayName("POST /api/auth/login - bad credentials")
    void login_BadCredentials() throws Exception {
        UserLoginDto loginDto = buildUserLoginDto();
        loginDto.setPassword(SAMPLE_WRONG_PASSWORD);

        when(authService.login(any(UserLoginDto.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post(API_LOGIN)
                        .contentType(APPLICATION_JSON)
                        .with(csrf())
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/logout - success")
    void logout_Success() throws Exception {
        mockMvc.perform(post(API_LOGOUT)
                        .with(csrf()))
                .andExpect(status().isNoContent());
        verify(authService).logout();
    }

    @Test
    @DisplayName("POST /api/auth/change-password - success")
    void changePassword_Success() throws Exception {
        UserChangePasswordDto changePasswordDto = buildUserChangePasswordDto();

        mockMvc.perform(post(API_CHANGE_PASSWORD)
                        .contentType(APPLICATION_JSON)
                        .with(csrf())
                        .content(objectMapper.writeValueAsString(changePasswordDto)))
                .andExpect(status().isOk());

        verify(authService).changePassword(OLD_PASSWORD, NEW_PASSWORD);
    }

    private @NotNull UserRegistrationDto buildUserRegistrationDto() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername(SAMPLE_USERNAME);
        dto.setPassword(SAMPLE_PASSWORD);
        dto.setEmail(SAMPLE_EMAIL);
        dto.setFirstName(SAMPLE_FIRST_NAME);
        dto.setLastName(SAMPLE_LAST_NAME);
        return dto;
    }

    private @NotNull UserLoginDto buildUserLoginDto() {
        UserLoginDto dto = new UserLoginDto();
        dto.setLogin(SAMPLE_USERNAME);
        dto.setPassword(SAMPLE_PASSWORD);
        return dto;
    }

    private @NotNull UserChangePasswordDto buildUserChangePasswordDto() {
        UserChangePasswordDto dto = new UserChangePasswordDto();
        dto.setOldPassword(OLD_PASSWORD);
        dto.setNewPassword(NEW_PASSWORD);
        return dto;
    }

    @Contract(" -> new")
    private @NotNull UserResponseDto buildUserResponseDto() {
        return new UserResponseDto(SAMPLE_USERNAME,
                SAMPLE_EMAIL,
                SAMPLE_FIRST_NAME,
                SAMPLE_LAST_NAME,
                null,
                0.0f,
                "USER");
    }
}