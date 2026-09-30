package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.user.LoginRequestDto;
import com.frigus.coreapi.dto.user.LoginResponseDto;
import com.frigus.coreapi.dto.user.UserRegisterRequestDto;
import com.frigus.coreapi.dto.user.UserResponseDto;
import com.frigus.coreapi.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    @Mock private AuthService authService;
    @Mock private HttpServletResponse response;
    @InjectMocks private AuthController controller;

    @Test
    void delegatesRegistrationAndLoginAndSetsSecureCookies() {
        UserRegisterRequestDto registration = UserRegisterRequestDto.builder().build();
        UserResponseDto user = UserResponseDto.builder().build();
        when(authService.register(registration)).thenReturn(user);
        assertThat(controller.register(registration)).isSameAs(user);

        LoginRequestDto login = LoginRequestDto.builder().build();
        LoginResponseDto tokens = LoginResponseDto.builder().accessToken("access").refreshToken("refresh").build();
        when(authService.login(login)).thenReturn(tokens);
        assertThat(controller.login(login, response)).isSameAs(tokens);
        ArgumentCaptor<String> cookies = ArgumentCaptor.forClass(String.class);
        verify(response, times(2)).addHeader(eq(HttpHeaders.SET_COOKIE), cookies.capture());
        assertThat(cookies.getAllValues()).allSatisfy(cookie -> assertThat(cookie).contains("HttpOnly", "SameSite=Lax"));
    }

    @Test
    void refreshAndLogoutPreferCookieThenFallbackToBodyAndClearCookies() {
        LoginResponseDto tokens = LoginResponseDto.builder().accessToken("new-access").refreshToken("new-refresh").build();
        when(authService.refreshToken("cookie-token")).thenReturn(tokens);
        assertThat(controller.refresh("cookie-token", Map.of("refreshToken", "body-token"), response)).isSameAs(tokens);
        verify(authService).refreshToken("cookie-token");
        controller.logout("", Map.of("refreshToken", "body-token"), response);
        verify(authService).logout("body-token");
        ArgumentCaptor<String> cookies = ArgumentCaptor.forClass(String.class);
        verify(response, times(4)).addHeader(eq(HttpHeaders.SET_COOKIE), cookies.capture());
        assertThat(cookies.getAllValues().subList(2, 4)).allSatisfy(cookie -> assertThat(cookie).contains("Max-Age=0"));
    }
}
