package com.frigus.coreapi.security;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.frigus.coreapi.enums.Role;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.UserRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityFilterTest {
    @Mock private TokenProvider tokenProvider;
    @Mock private UserRepository userRepository;
    @Mock private FilterChain chain;
    @InjectMocks private SecurityFilter filter;

    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    @Test
    void authenticatesAValidBearerTokenWithRoleAndMappedPlan() throws Exception {
        UUID id = UUID.randomUUID();
        DecodedJWT jwt = mock(DecodedJWT.class);
        Claim plan = mock(Claim.class);
        when(jwt.getSubject()).thenReturn(id.toString());
        when(jwt.getClaim("plan")).thenReturn(plan);
        when(plan.asString()).thenReturn("Frigus Família");
        when(tokenProvider.validateAccessToken("token")).thenReturn(jwt);
        when(userRepository.findById(id)).thenReturn(Optional.of(User.builder().id(id).role(Role.ADMIN).build()));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isInstanceOf(User.class);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString).contains("ROLE_ADMIN", "PLAN_DOMESTIC_FAMILY_BASIC");
        verify(chain).doFilter(any(), any());
    }

    @Test
    void passesThroughWhenThereIsNoTokenOrTheTokenIsInvalid() throws Exception {
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid");
        when(tokenProvider.validateAccessToken("invalid")).thenReturn(null);
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain, times(2)).doFilter(any(), any());
    }
}
