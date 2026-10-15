package com.frigus.coreapi.service;

import com.frigus.coreapi.enums.Role;
import com.frigus.coreapi.exception.ForbiddenException;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.UserGroupRepository;
import com.frigus.coreapi.repository.GroupRepository;
import com.frigus.coreapi.model.Group;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupAccessServiceTest {
    @Mock UserGroupRepository memberships;
    @Mock GroupRepository groups;
    @InjectMocks GroupAccessService service;

    @AfterEach
    void clearContext() { SecurityContextHolder.clearContext(); }

    @Test
    void unauthenticatedAccessFailsBeforeMembershipLookup() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> service.requireGroupAccess(UUID.randomUUID())).isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(memberships);
    }

    @Test
    void administratorCanAccessGroupWithoutMembership() {
        User admin = authenticate(Role.ADMIN);
        UUID groupId = UUID.randomUUID();
        when(groups.findByIdAndDeletedAtIsNull(groupId)).thenReturn(Optional.of(Group.builder().id(groupId).build()));
        service.requireGroupAccess(groupId);
        assertThat(service.requireCurrentUser()).isSameAs(admin);
        verifyNoInteractions(memberships);
    }

    @Test
    void memberAccessRequiresMembershipInTheRequestedGroup() {
        User user = authenticate(Role.USER);
        UUID allowed = UUID.randomUUID();
        UUID forbidden = UUID.randomUUID();
        when(groups.findByIdAndDeletedAtIsNull(allowed)).thenReturn(Optional.of(Group.builder().id(allowed).build()));
        when(groups.findByIdAndDeletedAtIsNull(forbidden)).thenReturn(Optional.of(Group.builder().id(forbidden).build()));
        when(memberships.existsByUserIdAndGroupId(user.getId(), allowed)).thenReturn(true);
        service.requireGroupAccess(allowed);
        assertThatThrownBy(() -> service.requireGroupAccess(forbidden))
                .isInstanceOf(ForbiddenException.class).hasMessage("Usuário não pertence ao grupo informado");
    }

    private User authenticate(Role role) {
        User user = User.builder().id(UUID.randomUUID()).role(role).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        return user;
    }
}
