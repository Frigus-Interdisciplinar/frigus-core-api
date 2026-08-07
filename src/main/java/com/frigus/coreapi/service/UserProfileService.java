package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.user.UserChangePasswordRequestDto;
import com.frigus.coreapi.dto.user.UserPatchRequestDto;
import com.frigus.coreapi.dto.user.UserPutRequestDto;
import com.frigus.coreapi.dto.user.UserResponseDto;
import com.frigus.coreapi.exception.BadRequestException;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.mapper.UserMapper;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository repository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;


    public UserResponseDto getProfile(User currentUser) {
        return mapper.toDto(currentUser);
    }

    public UserResponseDto updateProfile(User currentUser, UserPutRequestDto dto) {
        User user = repository.findById(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado", "Erro ao atualizar dados"));

        user.setName(dto.getName());
        user.setBirthDate(dto.getBirthDate());

        User savedUser = repository.save(user);
        return mapper.toDto(savedUser);
    }

    public UserResponseDto patchProfile(User currentUser, UserPatchRequestDto dto) {
        User user = repository.findById(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado", "Erro ao atualizar dados"));

        if (dto.getName() != null && !dto.getName().isBlank()) {
            user.setName(dto.getName());
        }
        if (dto.getBirthDate() != null) {
            user.setBirthDate(dto.getBirthDate());
        }

        User savedUser = repository.save(user);
        return mapper.toDto(savedUser);
    }

    public void changePassword(User currentUser, UserChangePasswordRequestDto dto) {
        User user = repository.findById(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado", "Erro ao resetar senha"));

        if (!passwordEncoder.matches(dto.getOldPassword(), user.getHashPassword())) {
            throw new BadRequestException("Senha atual incorreta", "A senha atual informada está incorreta");
        }

        if (passwordEncoder.matches(dto.getNewPassword(), user.getHashPassword())) {
            throw new BadRequestException("Nova senha igual à atual", "A nova senha deve ser diferente da senha atual");
        }

        user.setHashPassword(passwordEncoder.encode(dto.getNewPassword()));
        repository.save(user);
    }

    public void deleteCurrentUser(User currentUser) {
        User user = repository.findById(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado", "Erro ao excluir conta"));
        repository.delete(user);
    }
}
