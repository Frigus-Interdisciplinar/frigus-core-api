package com.frigus.coreapi.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.frigus.coreapi.model.User;
import java.util.UUID;

public class ServiceUtils {
    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        return null;
    }

    public static UUID getCurrentSessionId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getDetails() instanceof String sessionId)) return null;
        try {
            return UUID.fromString(sessionId);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
