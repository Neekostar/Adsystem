package com.neekostar.adsystem.service;

import com.neekostar.adsystem.dto.UserResponseDto;
import com.neekostar.adsystem.dto.UserUpdateDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {
    UserResponseDto getUserByUsername(String username);

    Page<UserResponseDto> getAllUsers(Pageable pageable);

    UserResponseDto updateUser(String username, UserUpdateDto userUpdateDto);

    void deleteUser(String username);

    UserResponseDto uploadUserAvatar(String username, MultipartFile file);

    void removeUserAvatar(String username);
}
