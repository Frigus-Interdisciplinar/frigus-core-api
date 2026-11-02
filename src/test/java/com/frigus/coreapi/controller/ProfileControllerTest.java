package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.user.*;
import com.frigus.coreapi.enums.AccountType;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {
    @Mock private UserProfileService service;
    @InjectMocks private ProfileController controller;

    @Test
    void delegatesAllProfileEndpoints() {
        User user = User.builder().id(UUID.randomUUID()).build();
        UserResponseDto response = UserResponseDto.builder().id(user.getId()).build();
        UserAccountTypeUpdateDto accountType = UserAccountTypeUpdateDto.builder()
                .accountType(com.frigus.coreapi.enums.AccountType.COMMERCIAL)
                .build();
        UserPutRequestDto put = UserPutRequestDto.builder().build();
        UserPatchRequestDto patch = UserPatchRequestDto.builder().build();
        UserChangePasswordRequestDto password = UserChangePasswordRequestDto.builder().build();
        UserAccountTypeUpdateDto accountType = UserAccountTypeUpdateDto.builder().accountType(AccountType.COMMERCIAL).build();
        when(service.getProfile(user)).thenReturn(response);
        when(service.updateOnboardingAccountType(user, accountType)).thenReturn(response);
        when(service.updateProfile(user, put)).thenReturn(response);
        when(service.patchProfile(user, patch)).thenReturn(response);

        assertThat(controller.getProfile(user)).isSameAs(response);
        assertThat(controller.updateOnboardingAccountType(user, accountType)).isSameAs(response);
        assertThat(controller.updateProfile(user, put)).isSameAs(response);
        assertThat(controller.patchProfile(user, patch)).isSameAs(response);
        controller.changePassword(user, password);
        controller.deleteCurrentUser(user);
        verify(service).changePassword(user, password);
        verify(service).updateOnboardingAccountType(user, accountType);
        verify(service).deleteCurrentUser(user);
    }
}
