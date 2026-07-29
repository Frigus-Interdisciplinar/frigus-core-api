package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.user.UserPatchRequestDto;
import com.frigus.coreapi.dto.user.UserPutRequestDto;
import com.frigus.coreapi.dto.user.UserRegisterRequestDto;
import com.frigus.coreapi.dto.user.UserResponseDto;
import com.frigus.coreapi.mapper.UserMapper;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/user")
public class UserController extends BaseController<User, UUID, UserRegisterRequestDto, UserResponseDto, UserService> {

    private final UserMapper userMapper;
    
    protected UserController(UserService service, UserMapper userMapper) {
        super(service);
        this.userMapper = userMapper;
    }

    @GetMapping("/me")
    public UserResponseDto getProfile(@AuthenticationPrincipal User currentUser) {
        return userMapper.toDto(currentUser);
    }

    @PutMapping("/me")
    public UserResponseDto updateProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UserPutRequestDto dto) {
        return service.updateProfile(currentUser, dto);
    }

    @PatchMapping("/me")
    public UserResponseDto patchProfile(
            @AuthenticationPrincipal User currentUser,
            @RequestBody UserPatchRequestDto dto) {
        return service.patchProfile(currentUser, dto);
    }
}
