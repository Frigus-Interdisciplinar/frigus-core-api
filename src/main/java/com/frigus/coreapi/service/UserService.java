package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.user.UserRegisterRequestDto;
import com.frigus.coreapi.dto.user.UserResponseDto;
import com.frigus.coreapi.mapper.UserMapper;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService extends BaseService<User, UUID, UserRegisterRequestDto, UserResponseDto, UserMapper, UserRepository> {
    public UserService(UserRepository repository, UserMapper mapper) {
        super(repository, mapper);
    }
}
