package com.frigus.coreapi.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.frigus.coreapi.model.User;

public class ServiceUtils {
    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        return null;
    }
}
