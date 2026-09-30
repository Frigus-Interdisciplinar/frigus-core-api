package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.user.*;
import com.frigus.coreapi.exception.BadRequestException;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {
    @Mock private UserRepository repository;
    @Mock private UserMapper mapper;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UserProfileService service;
    private User user;

    @BeforeEach void setUp() {
        user = User.builder().id(UUID.randomUUID()).name("Before").birthDate(LocalDate.of(1990, 1, 1)).hashPassword("hash").build();
    }

    @Test
    void getsAndUpdatesProfileIncludingPartialUpdates() {
        UserResponseDto response = UserResponseDto.builder().id(user.getId()).name("After").build();
        when(mapper.toDto(user)).thenReturn(response);
        assertThat(service.getProfile(user)).isSameAs(response);
        when(repository.findById(user.getId())).thenReturn(Optional.of(user));
        when(repository.save(user)).thenReturn(user);

        assertThat(service.updateProfile(user, UserPutRequestDto.builder().name("After").birthDate(LocalDate.of(1991, 2, 2)).build())).isSameAs(response);
        assertThat(user.getName()).isEqualTo("After");
        service.patchProfile(user, UserPatchRequestDto.builder().name(" ").build());
        assertThat(user.getName()).isEqualTo("After");
        service.patchProfile(user, UserPatchRequestDto.builder().birthDate(LocalDate.of(1992, 3, 3)).build());
        assertThat(user.getBirthDate()).isEqualTo(LocalDate.of(1992, 3, 3));
    }

    @Test
    void handlesPasswordChangesAndMissingUsers() {
        when(repository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "hash")).thenReturn(true);
        when(passwordEncoder.matches("new", "hash")).thenReturn(false);
        when(passwordEncoder.encode("new")).thenReturn("new-hash");
        service.changePassword(user, UserChangePasswordRequestDto.builder().oldPassword("old").newPassword("new").build());
        assertThat(user.getHashPassword()).isEqualTo("new-hash");

        when(passwordEncoder.matches("bad", "new-hash")).thenReturn(false);
        assertThatThrownBy(() -> service.changePassword(user, UserChangePasswordRequestDto.builder().oldPassword("bad").newPassword("next").build()))
                .isInstanceOf(BadRequestException.class);
        when(passwordEncoder.matches("old2", "new-hash")).thenReturn(true);
        when(passwordEncoder.matches("new-hash", "new-hash")).thenReturn(true);
        assertThatThrownBy(() -> service.changePassword(user, UserChangePasswordRequestDto.builder().oldPassword("old2").newPassword("new-hash").build()))
                .isInstanceOf(BadRequestException.class);

        User missing = User.builder().id(UUID.randomUUID()).build();
        when(repository.findById(missing.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteCurrentUser(missing)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void deletesThePersistedCurrentUser() {
        when(repository.findById(user.getId())).thenReturn(Optional.of(user));
        service.deleteCurrentUser(user);
        verify(repository).delete(user);
    }
}
