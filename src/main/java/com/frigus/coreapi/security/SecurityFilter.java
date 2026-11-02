package com.frigus.coreapi.security;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.UserRepository;
import com.frigus.coreapi.service.RefreshTokenService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(SecurityFilter.class);

    private final TokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = recoverToken(request);

        if (token != null) {
            DecodedJWT decodedJWT = tokenProvider.validateAccessToken(token);
            
            if (decodedJWT != null) {
                String userId = decodedJWT.getSubject();
                String planName = decodedJWT.getClaim("plan").asString();
                java.util.UUID sessionId = tokenProvider.sessionId(decodedJWT);
                
                User user = userRepository.findById(UUID.fromString(userId)).orElse(null);

                boolean sessionActive = sessionId == null || refreshTokenService.touchSession(sessionId);
                if (user != null && user.getDeletedAt()==null && tokenVersion(decodedJWT)==user.getTokenVersion() && sessionActive) {
                    var authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + user.getRole().name()),
                        new SimpleGrantedAuthority("PLAN_" + switchPlanName(planName))
                    );

                    var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
                    if (sessionId != null) authentication.setDetails(sessionId.toString());

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    recordFirstDailyAccess(user.getId());
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void recordFirstDailyAccess(UUID userId) {
        try {
            jdbcTemplate.update("""
                INSERT INTO dw.user_daily_access (user_id, access_date, first_access_at)
                VALUES (?, (CURRENT_TIMESTAMP AT TIME ZONE 'America/Sao_Paulo')::date, CURRENT_TIMESTAMP)
                ON CONFLICT (user_id, access_date) DO NOTHING
                """, userId);
        } catch (RuntimeException ex) {
            // Falha da metrica nao deve bloquear uma requisicao autenticada.
            log.warn("Nao foi possivel registrar o acesso diario no Data Mart", ex);
        }
    }
        

    private String recoverToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    } 

    private String switchPlanName(String rawPlanName) {
        if(rawPlanName==null) return "DOMESTIC_FREE";
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

    private int tokenVersion(DecodedJWT jwt) {
        var claim=jwt.getClaim("tokenVersion");
        Integer version=claim==null ? null : claim.asInt();
        return version==null ? 0 : version;
    }
}
