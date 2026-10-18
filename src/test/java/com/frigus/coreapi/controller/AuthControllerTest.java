package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.user.LoginRequestDto;
import com.frigus.coreapi.dto.user.LoginResponseDto;
import com.frigus.coreapi.dto.user.UserRegisterRequestDto;
import com.frigus.coreapi.dto.user.UserResponseDto;
import com.frigus.coreapi.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    @Mock private AuthService authService;
    @InjectMocks private AuthController controller;

    @Test
    void delegatesRegistrationAndLoginWithoutIssuingBrowserCookies() {
        UserRegisterRequestDto registration = UserRegisterRequestDto.builder().build();
        UserResponseDto user = UserResponseDto.builder().build();
        when(authService.register(registration)).thenReturn(user);
        assertThat(controller.register(registration)).isSameAs(user);

        LoginRequestDto login = LoginRequestDto.builder().build();
        LoginResponseDto tokens = LoginResponseDto.builder().accessToken("access").refreshToken("refresh").build();
        when(authService.login(login)).thenReturn(tokens);
        assertThat(controller.login(login)).isSameAs(tokens);
    }

    @Test
    void refreshAndLogoutUseBearerTokenValuesFromRequestBody() {
        LoginResponseDto tokens = LoginResponseDto.builder().accessToken("new-access").refreshToken("new-refresh").build();
        when(authService.refreshToken("body-token")).thenReturn(tokens);
        assertThat(controller.refresh(Map.of("refreshToken", "body-token"))).isSameAs(tokens);
        verify(authService).refreshToken("body-token");
        controller.logout(Map.of("refreshToken", "body-token"));
        verify(authService).logout("body-token");
    }
}
