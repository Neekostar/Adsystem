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
import com.neekostar.adsystem.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           UserMapper userMapper,
                           AuthenticationManager authenticationManager,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserResponseDto register(@NotNull UserRegistrationDto userRegistrationDto) {
        log.info("Attempting to register new user: {}", userRegistrationDto.getUsername());
        if (userRepository.existsUserByUsername(userRegistrationDto.getUsername())) {
            log.warn("Registration failed - username already exists: {}", userRegistrationDto.getUsername());
            throw new IllegalArgumentException("User with username " + userRegistrationDto.getUsername() + " already exists");
        }

        if (userRepository.existsUserByEmail(userRegistrationDto.getEmail())) {
            log.warn("Registration failed - email already exists: {}", userRegistrationDto.getEmail());
            throw new IllegalArgumentException("User with email " + userRegistrationDto.getEmail() + " already exists");
        }

        Role userRole = roleRepository.findRoleByName("USER")
                .orElseThrow(() -> {
                    log.error("Role 'USER' not found in database");
                    return new ResourceNotFoundException("Role", "name", "USER");
                });

        User user = new User();
        user.setUsername(userRegistrationDto.getUsername());
        user.setEmail(userRegistrationDto.getEmail());
        user.setPassword(passwordEncoder.encode(userRegistrationDto.getPassword()));
        user.setRole(userRole);
        user.setFirstName(userRegistrationDto.getFirstName());
        user.setLastName(userRegistrationDto.getLastName());

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getUsername());

        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto login(@NotNull UserLoginDto loginDto) {
        log.info("Login attempt for: {}", loginDto.getLogin());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginDto.getLogin(),
                            loginDto.getPassword()
                    )
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            String username = ((CustomUserDetails) authentication.getPrincipal()).getUsername();
            log.info("User logged in successfully: {}", username);

            return userRepository.findUserByUsername(username)
                    .map(user -> {
                        log.debug("Returning user data for: {}", username);
                        return userMapper.toDto(user);
                    })
                    .orElseThrow(() -> {
                        log.error("User not found after successful authentication: {}", username);
                        return new UnauthorizedException("Invalid login credentials");
                    });

        } catch (BadCredentialsException e) {
            log.warn("Invalid login attempt for: {}", loginDto.getLogin());
            throw new UnauthorizedException("Invalid login credentials");
        }
    }

    @Override
    public void logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            log.info("User logging out: {}", username);
        } else {
            log.warn("Logout attempt with no active authentication");
        }
        SecurityContextHolder.clearContext();
    }

    @Override
    @Transactional
    public void changePassword(@NotNull String oldPassword, String newPassword) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Password change request for user: {}", username);

        if (oldPassword.equals(newPassword)) {
            log.warn("Password change failed - same password for user: {}", username);
            throw new IllegalArgumentException("New password must be different from the old one");
        }

        if (newPassword == null || newPassword.isBlank()) {
            log.warn("Password change failed - new password is blank for user: {}", username);
            throw new IllegalArgumentException("New password must not be blank");
        }

        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found during password change: {}", username);
                    return new ResourceNotFoundException("User", "username", username);
                });

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            log.warn("Invalid old password for user: {}", username);
            throw new IllegalArgumentException("Invalid old password");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed successfully for user: {}", username);
    }
}
