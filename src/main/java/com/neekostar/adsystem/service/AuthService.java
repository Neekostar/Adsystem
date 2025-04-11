package com.neekostar.adsystem.service;

import com.neekostar.adsystem.dto.UserLoginDto;
import com.neekostar.adsystem.dto.UserRegistrationDto;
import com.neekostar.adsystem.dto.UserResponseDto;

public interface AuthService {
    UserResponseDto register(UserRegistrationDto userRegistrationDto);

    UserResponseDto login(UserLoginDto userLoginDto);

    void logout();

    void changePassword(String oldPassword, String newPassword);
}
