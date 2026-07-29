package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.user.UserPatchRequestDto;
import com.frigus.coreapi.dto.user.UserPutRequestDto;
import com.frigus.coreapi.dto.user.UserRegisterRequestDto;
import com.frigus.coreapi.dto.user.UserResponseDto;
import com.frigus.coreapi.mapper.UserMapper;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.UserRepository;
import com.frigus.coreapi.utils.ServiceUtils;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService extends BaseService<User, UUID, UserRegisterRequestDto, UserResponseDto, UserMapper, UserRepository> {
    public UserService(UserRepository repository, UserMapper mapper) {
        super(repository, mapper);
    }

    public UserResponseDto updateProfile(User currentUser, UserPutRequestDto dto) {
        currentUser.setName(dto.getName());
        currentUser.setBirthDate(dto.getBirthDate());

        User savedUser = repository.save(currentUser);
        return mapper.toDto(savedUser);
    }

    public UserResponseDto patchProfile(User currentUser, UserPatchRequestDto dto) {
        if (dto.getName() != null && !dto.getName().isBlank()) {
            currentUser.setName(dto.getName());
        }
        if (dto.getBirthDate() != null) {
            currentUser.setBirthDate(dto.getBirthDate());
        }

        User savedUser = repository.save(currentUser);
        return mapper.toDto(savedUser);
    }

    public UserResponseDto getProfile() {
        return mapper.toDto(ServiceUtils.getCurrentUser());
    }
}
