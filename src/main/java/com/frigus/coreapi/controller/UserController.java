package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.user.UserRegisterRequestDto;
import com.frigus.coreapi.dto.user.UserResponseDto;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.service.UserService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/user")
public class UserController extends BaseController<User, UUID, UserRegisterRequestDto, UserResponseDto, UserService> {
    protected UserController(UserService service) {
        super(service);
    }

}
