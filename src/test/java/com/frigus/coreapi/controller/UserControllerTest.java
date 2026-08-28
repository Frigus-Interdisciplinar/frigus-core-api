package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.user.*;
import com.frigus.coreapi.enums.AccountType;
import com.frigus.coreapi.enums.Role;
import com.frigus.coreapi.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {
    @Mock private UserService service;
    @InjectMocks private UserController controller;

    @Test
    void delegatesBaseAndAdministrativeUserEndpoints() {
        UUID id = UUID.randomUUID();
        UserResponseDto user = UserResponseDto.builder().id(id).build();
        PageRequest page = PageRequest.of(0, 10);
        when(service.findAll(page)).thenReturn(new PageImpl<>(List.of(user)));
        when(service.findById(id)).thenReturn(user);
        when(service.findByEmail("user@frigus.com")).thenReturn(user);
        when(service.updateUserRole(eq(id), any())).thenReturn(user);
        when(service.updateAccountType(eq(id), any())).thenReturn(user);

        assertThat(controller.findAll(page).getContent()).containsExactly(user);
        assertThat(controller.findById(id)).isSameAs(user);
        assertThat(controller.findByEmail("user@frigus.com")).isSameAs(user);
        assertThat(controller.updateUserRole(id, UserRoleUpdateDto.builder().role(Role.ADMIN).build())).isSameAs(user);
        assertThat(controller.updateAccountType(id, UserAccountTypeUpdateDto.builder().accountType(AccountType.COMMERCIAL).build())).isSameAs(user);
        controller.adminResetPassword(id, UserAdminResetPasswordDto.builder().newPassword("password").build());
        controller.delete(id);
        verify(service).adminResetPassword(eq(id), any(UserAdminResetPasswordDto.class));
        verify(service).delete(id);
    }
}
