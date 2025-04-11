package com.neekostar.adsystem.service.impl;

import com.neekostar.adsystem.dto.UserLoginDto;
import com.neekostar.adsystem.dto.UserRegistrationDto;
import com.neekostar.adsystem.dto.UserResponseDto;
import com.neekostar.adsystem.exception.ResourceNotFoundException;
import com.neekostar.adsystem.exception.UnauthorizedException;
import com.neekostar.adsystem.mapper.UserMapper;
import com.neekostar.adsystem.model.Role;
import com.neekostar.adsystem.model.User;
import com.neekostar.adsystem.repository.RoleRepository;
import com.neekostar.adsystem.repository.UserRepository;
import com.neekostar.adsystem.security.CustomUserDetails;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AuthServiceImplTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    private UserRegistrationDto validRegistrationDto;
    private UserLoginDto validLoginDto;
    private User testUser;
    private Role userRole;

    @BeforeEach
    void setup() {
        userRole = new Role();
        userRole.setName("USER");

        validRegistrationDto = new UserRegistrationDto();
        validRegistrationDto.setUsername("newUser");
        validRegistrationDto.setPassword("password");
        validRegistrationDto.setEmail("test@example.com");
        validRegistrationDto.setFirstName("John");
        validRegistrationDto.setLastName("Doe");

        validLoginDto = new UserLoginDto();
        validLoginDto.setLogin("user");
        validLoginDto.setPassword("validPassword");

        testUser = new User();
        testUser.setUsername("user");
        testUser.setPassword("encodedPass");
        testUser.setEmail("user@mail.com");
        testUser.setRole(userRole);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void register_WithValidData_ShouldReturnUserResponseDto() {
        when(roleRepository.findRoleByName("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        when(userRepository.save(any())).thenReturn(testUser);
        when(userMapper.toDto(testUser)).thenReturn(createResponseDto(testUser));

        UserResponseDto result = authService.register(validRegistrationDto);

        assertResponseDtoMatches(createResponseDto(testUser), result);
        verify(userRepository).save(any());
        verify(userRepository).existsUserByUsername(validRegistrationDto.getUsername());
        verify(userRepository).existsUserByEmail(validRegistrationDto.getEmail());
    }

    @Test
    void register_WithExistingUsername_ShouldThrowIllegalArgumentException() {
        when(userRepository.existsUserByUsername(validRegistrationDto.getUsername())).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> authService.register(validRegistrationDto));

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_WithExistingEmail_ShouldThrowIllegalArgumentException() {
        when(userRepository.existsUserByEmail(validRegistrationDto.getEmail())).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> authService.register(validRegistrationDto));
    }

    @Test
    void register_WhenRoleNotFound_ShouldThrowResourceNotFoundException() {
        when(roleRepository.findRoleByName("USER")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> authService.register(validRegistrationDto));
    }

    @Test
    void login_WithValidCredentials_ShouldReturnUserResponseDto() {
        Authentication auth = mock(Authentication.class);
        CustomUserDetails userDetails = new CustomUserDetails(testUser);

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findUserByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));
        when(userMapper.toDto(testUser)).thenReturn(createResponseDto(testUser));

        UserResponseDto result = authService.login(validLoginDto);

        assertResponseDtoMatches(createResponseDto(testUser), result);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void login_WithInvalidCredentials_ShouldThrowUnauthorizedException() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThrows(UnauthorizedException.class,
                () -> authService.login(validLoginDto));
    }

    @Test
    void login_WhenUserNotFoundAfterAuth_ShouldThrowUnauthorizedException() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(new CustomUserDetails(testUser));

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findUserByUsername(any())).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class,
                () -> authService.login(validLoginDto));
    }

    @Test
    void changePassword_WithValidData_ShouldUpdatePassword() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser.getUsername(), null)
        );

        when(userRepository.findUserByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPass");

        authService.changePassword("oldPassword", "newPassword");

        assertEquals("newEncodedPass", testUser.getPassword());
        verify(userRepository).save(testUser);
    }

    @Test
    void changePassword_WithSamePassword_ShouldThrowIllegalArgumentException() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser.getUsername(), null)
        );

        assertThrows(IllegalArgumentException.class,
                () -> authService.changePassword("password", "password"));
    }

    @Test
    void changePassword_WithWrongOldPassword_ShouldThrowIllegalArgumentException() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser.getUsername(), null)
        );
        when(userRepository.findUserByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> authService.changePassword("wrongPass", "newPass"));
    }

    @Test
    void changePassword_WhenUserNotFound_ShouldThrowResourceNotFoundException() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("unknown", null)
        );
        when(userRepository.findUserByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> authService.changePassword("oldPass", "newPass"));
    }

    @Test
    void changePassword_WithBlankNewPassword_ShouldThrowIllegalArgumentException() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser.getUsername(), null)
        );
        when(userRepository.findUserByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.changePassword("oldPassword", "   "));
        assertEquals("New password must not be blank", ex.getMessage());
    }

    @Test
    void changePassword_WithNullNewPassword_ShouldThrowIllegalArgumentException() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser.getUsername(), null)
        );
        when(userRepository.findUserByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.changePassword("oldPassword", null));
        assertEquals("New password must not be blank", ex.getMessage());
    }

    @Test
    void logout_ShouldClearSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser.getUsername(), null)
        );

        authService.logout();

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void logout_WithNonAuthenticatedUser_ShouldLogWarningAndClearContext() {
        Authentication nonAuthenticated = mock(Authentication.class);
        when(nonAuthenticated.isAuthenticated()).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(nonAuthenticated);

        authService.logout();

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void logout_WithAuthenticatedUser_ShouldLogInfoAndClearContext() {
        Authentication authenticated = mock(Authentication.class);
        when(authenticated.isAuthenticated()).thenReturn(true);
        when(authenticated.getName()).thenReturn("testUser");
        SecurityContextHolder.getContext().setAuthentication(authenticated);

        authService.logout();

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }


    private UserResponseDto createResponseDto(@NotNull User user) {
        return new UserResponseDto(
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getAvatarUrl(),
                user.getRating(),
                user.getRole().getName()
        );
    }

    private void assertResponseDtoMatches(@NotNull UserResponseDto expected, @NotNull UserResponseDto actual) {
        assertEquals(expected.getUsername(), actual.getUsername());
        assertEquals(expected.getEmail(), actual.getEmail());
        assertEquals(expected.getFirstName(), actual.getFirstName());
        assertEquals(expected.getLastName(), actual.getLastName());
        assertEquals(expected.getRole(), actual.getRole());
    }
}
