package com.frigus.coreapi.service;

import com.frigus.coreapi.client.EmailDeliveryException;
import com.frigus.coreapi.dto.group.AcceptInvitationRequestDto;
import com.frigus.coreapi.dto.group.GroupInvitationRequestDto;
import com.frigus.coreapi.dto.group.GroupInvitationResponseDto;
import com.frigus.coreapi.dto.group.GroupResponseDto;
import com.frigus.coreapi.exception.BadRequestException;
import com.frigus.coreapi.exception.ConflictException;
import com.frigus.coreapi.exception.ForbiddenException;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.exception.ServiceUnavailableException;
import com.frigus.coreapi.model.GroupInvitation;
import com.frigus.coreapi.repository.GroupInvitationRepository;
import com.frigus.coreapi.repository.GroupRepository;
import com.frigus.coreapi.repository.UserGroupRepository;
import com.frigus.coreapi.utils.OneTimeTokens;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupInvitationService {
    private final GroupInvitationRepository invitations;
    private final GroupRepository groups;
    private final GroupAccessService access;
    private final GroupService groupService;
    private final UserGroupRepository members;
    private final EmailService email;

    public List<GroupInvitationResponseDto> list(UUID groupId) {
        access.requireGroupOwnerAccess(groupId);
        return invitations.findByGroupIdOrderByCreatedAtDesc(groupId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public GroupInvitationResponseDto create(UUID groupId, GroupInvitationRequestDto request) {
        access.requireGroupOwnerAccess(groupId);
        var group = groups.findByIdForUpdate(groupId).orElseThrow(NotFoundException::new);
        String emailAddress = normalizeEmail(request.email());
        revokeExpiredPendingInvitation(groupId, emailAddress);

        String rawToken = OneTimeTokens.create();
        var invitation = invitations.save(GroupInvitation.builder()
                .group(group)
                .email(emailAddress)
                .name(request.name())
                .memberRole(request.memberRole())
                .tokenHash(OneTimeTokens.hash(rawToken))
                .expiresAt(Instant.now().plus(Duration.ofDays(7)))
                .build());

        sendInvitation(group.getName(), emailAddress, rawToken);
        return toDto(invitation);
    }

    @Transactional
    public void revoke(UUID groupId, UUID invitationId) {
        access.requireGroupOwnerAccess(groupId);
        var invitation = invitations.findById(invitationId).orElseThrow(NotFoundException::new);
        if (!invitation.getGroup().getId().equals(groupId)) {
            throw new NotFoundException();
        }
        if (invitation.getAcceptedAt() != null) {
            throw new ConflictException(
                    "Convite já aceito",
                    "Remova o membro pelo gerenciamento do grupo"
            );
        }

        invitation.setRevokedAt(Instant.now());
        invitations.save(invitation);
    }

    @Transactional
    public GroupResponseDto accept(AcceptInvitationRequestDto request) {
        var invitation = invitations.lockByHash(OneTimeTokens.hash(request.token()))
                .orElseThrow(this::invalidInvitation);
        var user = access.requireCurrentUser();

        if (!user.getEmail().equalsIgnoreCase(invitation.getEmail())) {
            throw new ForbiddenException("Convite para outro e-mail", "Entre com a conta que recebeu o convite");
        }
        if (invitation.getAcceptedAt() != null
                && members.existsByUserIdAndGroupId(user.getId(), invitation.getGroup().getId())) {
            return groupService.getGroupById(invitation.getGroup().getId());
        }
        if (invitation.getRevokedAt() != null
                || invitation.getAcceptedAt() != null
                || !invitation.getExpiresAt().isAfter(Instant.now())) {
            throw invalidInvitation();
        }

        var group = groupService.joinInvitedGroup(
                invitation.getGroup().getId(),
                invitation.getMemberRole()
        );
        invitation.setAcceptedAt(Instant.now());
        invitations.save(invitation);
        return group;
    }

    private void revokeExpiredPendingInvitation(UUID groupId, String emailAddress) {
        var existing = invitations.findByGroupIdAndEmailIgnoreCaseAndAcceptedAtIsNullAndRevokedAtIsNull(
                groupId,
                emailAddress
        );
        if (existing.isEmpty()) {
            return;
        }

        var invitation = existing.get();
        if (invitation.getExpiresAt().isAfter(Instant.now())) {
            throw new ConflictException(
                    "Convite pendente",
                    "Revogue o convite anterior antes de reenviar"
            );
        }

        invitation.setRevokedAt(Instant.now());
        invitations.saveAndFlush(invitation);
    }

    private void sendInvitation(String groupName, String emailAddress, String token) {
        try {
            email.sendLink(emailAddress, "Convite para " + groupName, "/accept-invitation", token);
        } catch (EmailDeliveryException exception) {
            throw new ServiceUnavailableException(
                    "Falha no envio do convite",
                    "Tente novamente em alguns instantes"
            );
        }
    }

    private String normalizeEmail(String emailAddress) {
        return emailAddress.trim().toLowerCase(Locale.ROOT);
    }

    private BadRequestException invalidInvitation() {
        return new BadRequestException("Convite inválido ou expirado", "Solicite um novo convite");
    }

    private GroupInvitationResponseDto toDto(GroupInvitation invitation) {
        return new GroupInvitationResponseDto(
                invitation.getId(),
                invitation.getGroup().getId(),
                invitation.getEmail(),
                invitation.getName(),
                invitation.getMemberRole(),
                invitation.getExpiresAt(),
                invitation.getAcceptedAt(),
                invitation.getRevokedAt()
        );
    }
}
