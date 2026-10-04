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

    public void requireGroupWriteAccess(UUID groupId) {
        requireGroupAccess(groupId);
        User current=requireCurrentUser();
        if(current.getRole()==Role.ADMIN) return;
        var membership=userGroupRepository.findByUserIdAndGroupId(current.getId(),groupId)
            .orElseThrow(() -> new ForbiddenException("Sem vínculo no grupo","Acesso negado"));
        if(membership.getGroup().getDeletedAt()!=null) throw new ForbiddenException("Grupo inativo","Acesso negado");
        if(membership.getGroup().getOwner()!=null && membership.getGroup().getOwner().getId().equals(current.getId())) return;
        if(membership.getMemberRole()==com.frigus.coreapi.enums.MemberRole.VIEWER)
            throw new ForbiddenException("Permissão somente de leitura","Você não pode editar este grupo");
    }
    public void requireGroupOwnerAccess(UUID groupId) {
        requireGroupAccess(groupId);
        User current=requireCurrentUser();
        if(current.getRole()==Role.ADMIN) return;
        var membership=userGroupRepository.findByUserIdAndGroupId(current.getId(),groupId)
            .orElseThrow(() -> new ForbiddenException("Sem vínculo no grupo","Acesso negado"));
        if(membership.getGroup().getDeletedAt()!=null || membership.getGroup().getOwner()==null || !membership.getGroup().getOwner().getId().equals(current.getId()))
            throw new ForbiddenException("Gerenciamento restrito ao proprietário","Acesso negado");
    }
}
