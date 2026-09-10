package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.user.UserChangePasswordRequestDto;
import com.frigus.coreapi.dto.user.UserPatchRequestDto;
import com.frigus.coreapi.dto.user.UserPutRequestDto;
import com.frigus.coreapi.dto.user.UserResponseDto;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile")
public class ProfileController {

    private final UserProfileService profileService;

    @GetMapping
    public UserResponseDto getProfile(@AuthenticationPrincipal User currentUser) {
        return profileService.getProfile(currentUser);
    }

    @PutMapping
    public UserResponseDto updateProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UserPutRequestDto dto) {
        return profileService.updateProfile(currentUser, dto);
    }

    @PatchMapping
    public UserResponseDto patchProfile(
            @AuthenticationPrincipal User currentUser,
            @RequestBody UserPatchRequestDto dto) {
        return profileService.patchProfile(currentUser, dto);
    }

    @PatchMapping("/password")
    public void changePassword(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UserChangePasswordRequestDto dto) {
        profileService.changePassword(currentUser, dto);
    }

    @DeleteMapping
    public void deleteCurrentUser(@AuthenticationPrincipal User currentUser) {
        profileService.deleteCurrentUser(currentUser);
    }
}
