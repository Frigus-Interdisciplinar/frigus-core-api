package com.frigus.coreapi.security;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {
    private final TokenProvider tokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = recoverToken(request);

        if (token != null) {
            DecodedJWT decodedJWT = tokenProvider.validateAccessToken(token);
            
            if (decodedJWT != null) {
                String userId = decodedJWT.getSubject();
                String planName = decodedJWT.getClaim("plan").asString();
                
                User user = userRepository.findById(UUID.fromString(userId)).orElse(null);

                if (user != null) {
                    var authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_PLAN_" + switchPlanName(planName))
                    );

                    var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
        

    private String recoverToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    } 

    // ! mudar para novos nomes qnd o 1o ano decidir
    private String switchPlanName(String rawPlanName) {
        switch (rawPlanName) {
            case "Frigus Free":
                return "DOMESTIC_FREE";
            case "Frigus Família":
                return "DOMESTIC_FAMILY_BASIC";
            case "Frigus Black":
                return "DOMESTIC_FAMILY_PRO";
            case "Frigus Comercial":
                return "COMMERCIAL";
            case "Frigus Empresarial":
                return "ENTERPRISE";
            default:
                return "DOMESTIC_FREE";
        }
    }
}
