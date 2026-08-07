package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.user.*;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.mapper.UserMapper;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService extends BaseService<User, UUID, UserRegisterRequestDto, UserResponseDto, UserMapper, UserRepository> {

    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, UserMapper mapper, PasswordEncoder passwordEncoder) {
        super(repository, mapper);
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponseDto findByEmail(String email) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado", "Nenhum usuário foi encontrado com o e-mail informado"));
        return mapper.toDto(user);
    }

    public UserResponseDto updateUserRole(UUID userId, UserRoleUpdateDto dto) {
        User user = repository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado", "Usuário não encontrado para atualização de role"));
        user.setRole(dto.getRole());
        User savedUser = repository.save(user);
        return mapper.toDto(savedUser);
    }

    public UserResponseDto updateAccountType(UUID userId, UserAccountTypeUpdateDto dto) {
        User user = repository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado", "Usuário não encontrado para atualização de tipo de conta"));
        user.setAccountType(dto.getAccountType());
        User savedUser = repository.save(user);
        return mapper.toDto(savedUser);
    }

    public void adminResetPassword(UUID userId, UserAdminResetPasswordDto dto) {
        User user = repository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado", "Usuário não encontrado para reset de senha"));
        user.setHashPassword(passwordEncoder.encode(dto.getNewPassword()));
        repository.save(user);
    }

    public void deleteUser(UUID userId) {
        delete(userId);
    }
}
