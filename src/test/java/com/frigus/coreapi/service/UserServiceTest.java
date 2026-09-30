package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.user.*;
import com.frigus.coreapi.enums.AccountType;
import com.frigus.coreapi.enums.Role;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.mapper.UserMapper;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock private UserRepository repository;
    @Mock private UserMapper mapper;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UserService service;
    private User user;
    private UserResponseDto dto;

    @BeforeEach void setUp() {
        user = User.builder().id(UUID.randomUUID()).email("user@frigus.com").role(Role.USER).accountType(AccountType.DOMESTIC).build();
        dto = UserResponseDto.builder().id(user.getId()).build();
    }

    @Test
    void findsUsersByEmailAndByIdAndListsThem() {
        when(repository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(repository.findById(user.getId())).thenReturn(Optional.of(user));
        when(repository.findAll(PageRequest.of(0, 10))).thenReturn(new PageImpl<>(java.util.List.of(user)));
        when(mapper.toDto(user)).thenReturn(dto);
        assertThat(service.findByEmail(user.getEmail())).isSameAs(dto);
        assertThat(service.findById(user.getId())).isSameAs(dto);
        assertThat(service.findAll(PageRequest.of(0, 10)).getContent()).containsExactly(dto);
        when(repository.findByEmail("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findByEmail("missing")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void updatesAdministrativeFieldsAndResetsPassword() {
        when(repository.findById(user.getId())).thenReturn(Optional.of(user));
        when(repository.save(user)).thenReturn(user);
        when(mapper.toDto(user)).thenReturn(dto);
        assertThat(service.updateUserRole(user.getId(), UserRoleUpdateDto.builder().role(Role.ADMIN).build())).isSameAs(dto);
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        assertThat(service.updateAccountType(user.getId(), UserAccountTypeUpdateDto.builder().accountType(AccountType.COMMERCIAL).build())).isSameAs(dto);
        assertThat(user.getAccountType()).isEqualTo(AccountType.COMMERCIAL);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");
        service.adminResetPassword(user.getId(), UserAdminResetPasswordDto.builder().newPassword("new-password").build());
        assertThat(user.getHashPassword()).isEqualTo("new-hash");
    }

    @Test
    void exposesNotFoundAndDeleteBehavior() {
        UUID missingId = UUID.randomUUID();
        when(repository.findById(missingId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateUserRole(missingId, UserRoleUpdateDto.builder().role(Role.ADMIN).build()))
                .isInstanceOf(NotFoundException.class);
        doThrow(new IllegalArgumentException()).when(repository).deleteById(missingId);
        assertThatThrownBy(() -> service.deleteUser(missingId)).isInstanceOf(NotFoundException.class);
    }
}
