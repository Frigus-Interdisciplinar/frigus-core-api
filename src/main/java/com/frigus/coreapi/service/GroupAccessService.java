package com.frigus.coreapi.service;

import com.frigus.coreapi.enums.Role;
import com.frigus.coreapi.exception.ForbiddenException;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.model.Group;
import com.frigus.coreapi.repository.GroupRepository;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.repository.UserGroupRepository;
import com.frigus.coreapi.utils.ServiceUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupAccessService {
    private final UserGroupRepository userGroupRepository;
    private final GroupRepository groupRepository;

    public User requireCurrentUser() {
        User user = ServiceUtils.getCurrentUser();
        if (user == null) {
            throw new ForbiddenException("Usuário não autenticado", "Acesso negado");
        }
        return user;
    }

    public void requireGroupAccess(UUID groupId) {
        User user = requireCurrentUser();
        requireActiveGroup(groupId);
        if (user.getRole() == Role.ADMIN) {
            return;
        }

        if (!userGroupRepository.existsByUserIdAndGroupId(user.getId(), groupId)) {
            throw new ForbiddenException(
                    "Usuário não pertence ao grupo informado",
                    "Você não tem acesso a este grupo");
        }
    }

    public Group requireActiveGroup(UUID groupId) {
        return groupRepository.findByIdAndDeletedAtIsNull(groupId)
                .orElseThrow(() -> new NotFoundException("Grupo não encontrado", "O grupo informado não está ativo"));
    }
}
