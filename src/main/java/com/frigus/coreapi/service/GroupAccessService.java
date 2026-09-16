package com.frigus.coreapi.service;

import com.frigus.coreapi.enums.Role;
import com.frigus.coreapi.exception.ForbiddenException;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.UserGroupRepository;
import com.frigus.coreapi.utils.ServiceUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupAccessService {
    private final UserGroupRepository userGroupRepository;

    public User requireCurrentUser() {
        User user = ServiceUtils.getCurrentUser();
        if (user == null) {
            throw new ForbiddenException("Usuário não autenticado", "Acesso negado");
        }
        return user;
    }

    public void requireGroupAccess(UUID groupId) {
        User user = requireCurrentUser();
        if (user.getRole() == Role.ADMIN) {
            return;
        }

        if (!userGroupRepository.existsByUserIdAndGroupId(user.getId(), groupId)) {
            throw new ForbiddenException(
                    "Usuário não pertence ao grupo informado",
                    "Você não tem acesso a este grupo");
        }
    }
}
