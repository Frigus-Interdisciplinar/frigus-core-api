package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.user.*;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.service.UserService;
import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/user")
public class UserController extends BaseController<User, UUID, UserRegisterRequestDto, UserResponseDto, UserService> {

    protected UserController(UserService service) {
        super(service);
    }

    @GetMapping("/search")
    public UserResponseDto findByEmail(@RequestParam String email) {
        return service.findByEmail(email);
    }

    @PatchMapping("/{id}/role")
    public UserResponseDto updateUserRole(
            @PathVariable UUID id,
            @Valid @RequestBody UserRoleUpdateDto dto) {
        return service.updateUserRole(id, dto);
    }

    @PatchMapping("/{id}/account-type")
    public UserResponseDto updateAccountType(
            @PathVariable UUID id,
            @Valid @RequestBody UserAccountTypeUpdateDto dto) {
        return service.updateAccountType(id, dto);
    }

    @PatchMapping("/{id}/password")
    public void adminResetPassword(
            @PathVariable UUID id,
            @Valid @RequestBody UserAdminResetPasswordDto dto) {
        service.adminResetPassword(id, dto);
    }
}
