package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.notification.NotificationResponseDto;
import com.frigus.coreapi.enums.AccountType;
import com.frigus.coreapi.enums.NotificationType;
import com.frigus.coreapi.exception.ForbiddenException;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.mapper.NotificationMapper;
import com.frigus.coreapi.model.Group;
import com.frigus.coreapi.model.Notification;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.model.UserGroup;
import com.frigus.coreapi.repository.NotificationRepository;
import com.frigus.coreapi.repository.UserGroupRepository;
import com.frigus.coreapi.utils.ServiceUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private static final long AD_CLICK_MILESTONE = 2_500L;

    private final NotificationRepository notificationRepository;
    private final UserGroupRepository userGroupRepository;
    private final NotificationMapper notificationMapper;

    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> listMyNotifications(Pageable pageable) {
        User currentUser = requireCurrentUser();
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(currentUser.getId(), pageable)
                .map(notificationMapper::toDto);
    }

    @Transactional
    public NotificationResponseDto markAsRead(UUID notificationId) {
        User currentUser = requireCurrentUser();
        Notification notification = notificationRepository.findByIdAndRecipientId(notificationId, currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Notificação não encontrada", "A notificação não existe para este usuário"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            notification = notificationRepository.save(notification);
        }
        return notificationMapper.toDto(notification);
    }

    /** Creates one notification for every current member of a domestic or commercial group. */
    @Transactional
    public void notifyDomesticOrCommercialGroup(
            Group group,
            NotificationType type,
            String title,
            String body,
            String referenceId,
            String deduplicationKeyPrefix) {
        if (!isDomesticOrCommercialGroup(group)) {
            return;
        }

        for (UserGroup membership : userGroupRepository.findByGroupId(group.getId())) {
            String key = deduplicationKeyPrefix == null ? null
                    : deduplicationKeyPrefix + ":" + membership.getUser().getId();
            if (key != null && notificationRepository.existsByDeduplicationKey(key)) {
                continue;
            }
            notificationRepository.save(Notification.builder()
                    .recipient(membership.getUser())
                    .group(group)
                    .type(type)
                    .title(title)
                    .body(body)
                    .referenceId(referenceId)
                    .deduplicationKey(key)
                    .createdAt(Instant.now())
                    .build());
        }
    }

    @Transactional
    public void notifyAdClickMilestones(long totalClicks) {
        User currentUser = requireCurrentUser();
        if (currentUser.getAccountType() != AccountType.BUSINESS) {
            throw new ForbiddenException("Recurso indisponível", "O aviso de cliques é exclusivo para contas empresariais");
        }

        for (long clicks = AD_CLICK_MILESTONE; clicks <= totalClicks; clicks += AD_CLICK_MILESTONE) {
            String key = "ad-click:" + currentUser.getId() + ":" + clicks;
            if (notificationRepository.existsByDeduplicationKey(key)) {
                continue;
            }
            notificationRepository.save(Notification.builder()
                    .recipient(currentUser)
                    .type(NotificationType.AD_CLICK_MILESTONE)
                    .title("Seu anúncio teve " + clicks + " cliques!")
                    .body("Seu anúncio alcançou " + clicks + " cliques.")
                    .referenceId(String.valueOf(clicks))
                    .deduplicationKey(key)
                    .createdAt(Instant.now())
                    .build());
        }
    }

    private boolean isDomesticOrCommercialGroup(Group group) {
        if (group == null || group.getOwner() == null) {
            return false;
        }
        AccountType accountType = group.getOwner().getAccountType();
        return accountType == AccountType.DOMESTIC || accountType == AccountType.COMMERCIAL;
    }

    private User requireCurrentUser() {
        User user = ServiceUtils.getCurrentUser();
        if (user == null) {
            throw new com.frigus.coreapi.exception.UnauthorizedException("Usuário não autenticado", "Faça login para continuar");
        }
        return user;
    }
}
