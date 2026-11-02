package com.frigus.coreapi.security;

import com.frigus.coreapi.dto.ErrorResponseDto;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;
import java.io.IOException;
import java.time.Instant;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, SecurityFilter securityFilter,
                                           RateLimitFilter rateLimitFilter,
                                           CorsConfigurationSource corsConfigurationSource,
                                           ObjectMapper objectMapper) throws Exception {
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookieCustomizer(cookie -> cookie.secure(true).sameSite("None"));
        http.cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .requireCsrfProtectionMatcher(request -> {
                            if (List.of("GET", "HEAD", "OPTIONS", "TRACE").contains(request.getMethod())) return false;
                            String path = request.getRequestURI().substring(request.getContextPath().length());
                            if (List.of("/auth/login", "/auth/register").contains(path)) return false;
                            String authorization = request.getHeader("Authorization");
                            if (!List.of("/auth/refresh", "/auth/logout").contains(path)
                                    && authorization != null && authorization.startsWith("Bearer ")) return false;
                            Cookie[] cookies = request.getCookies();
                            if (cookies == null) return false;
                            return Arrays.stream(cookies).anyMatch(cookie ->
                                    "accessToken".equals(cookie.getName()) || "refreshToken".equals(cookie.getName()));
                        }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/**",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/ws/**",
                                "/*.html",
                                "/static/**")
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/plans",
                                "/plans/active",
                                "/plans/*",
                                "/plans/code/*",
                                "/products",
                                "/products/*")
                        .permitAll()
                .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> writeProblem(
                                response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED,
                                "UNAUTHORIZED", "Autenticação necessária", "Faça login para continuar"))
                        .accessDeniedHandler((request, response, exception) -> writeProblem(
                                response, objectMapper, HttpServletResponse.SC_FORBIDDEN,
                                "FORBIDDEN", "Acesso negado", "Você não tem permissão para esta operação")))
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimitFilter, SecurityFilter.class);
        return http.build();
    }

    private void writeProblem(HttpServletResponse response, ObjectMapper objectMapper,
                              int status, String code,
                              String message, String displayMessage) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ErrorResponseDto(
                status, code, message, displayMessage, Instant.now()));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${SECURITY_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:5173}") String origins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-XSRF-TOKEN", "Accept"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}
