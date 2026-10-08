package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.notification.NotificationResponseDto;
import com.frigus.coreapi.enums.AccountType;
import com.frigus.coreapi.enums.NotificationType;
import com.frigus.coreapi.exception.ForbiddenException;
import com.frigus.coreapi.exception.BadRequestException;
import com.frigus.coreapi.exception.ServiceUnavailableException;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private static final long AD_CLICK_MILESTONE = 2_500L;

    private final NotificationRepository notificationRepository;
    private final UserGroupRepository userGroupRepository;
    private final NotificationMapper notificationMapper;
    private final JdbcTemplate jdbcTemplate;
    @Value("${AD_CLICK_REPORT_SECRET:}") private String reportSecret;

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
    public void notifyAdClickMilestones(long totalClicks, long timestamp, String signature) {
        User currentUser = requireCurrentUser();
        if (currentUser.getAccountType() != AccountType.BUSINESS) {
            throw new ForbiddenException("Recurso indisponível", "O aviso de cliques é exclusivo para contas empresariais");
        }
        if (reportSecret == null || reportSecret.isBlank()) {
            throw new ServiceUnavailableException("Relatórios indisponíveis", "A integração de cliques não está configurada");
        }
        if (totalClicks <= 0 || totalClicks > 1_000_000_000L
                || Math.abs(Instant.now().getEpochSecond() - timestamp) > 300) {
            throw new BadRequestException("Relatório inválido", "Contagem ou horário inválido");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(reportSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String payload = currentUser.getId() + ":" + totalClicks + ":" + timestamp;
            byte[] expected = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            if (!MessageDigest.isEqual(expected, HexFormat.of().parseHex(signature))) {
                throw new ForbiddenException("Relatório inválido", "Assinatura inválida");
            }
        } catch (IllegalArgumentException e) {
            throw new ForbiddenException("Relatório inválido", "Assinatura inválida");
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("HMAC unavailable", e);
        }

        Long latest = jdbcTemplate.queryForObject(
                "select coalesce(max(cast(reference_id as bigint)), 0) from notifications " +
                        "where recipient_user_id = ? and type = 'AD_CLICK_MILESTONE'",
                Long.class, currentUser.getId());
        long lastMilestone = latest == null ? 0 : latest;
        if (totalClicks - lastMilestone > 10_000L) {
            throw new BadRequestException("Avanço excessivo", "Envie um avanço de até 10000 cliques por relatório");
        }
        for (long clicks = lastMilestone + AD_CLICK_MILESTONE; clicks <= totalClicks; clicks += AD_CLICK_MILESTONE) {
            String key = "ad-click:" + currentUser.getId() + ":" + clicks;
            jdbcTemplate.update("insert into notifications " +
                            "(id, recipient_user_id, type, title, body, reference_id, deduplication_key, created_at) " +
                            "values (?, ?, 'AD_CLICK_MILESTONE', ?, ?, ?, ?, ?) " +
                            "on conflict (deduplication_key) where deduplication_key is not null do nothing",
                    UUID.randomUUID(), currentUser.getId(), "Seu anúncio teve " + clicks + " cliques!",
                    "Seu anúncio alcançou " + clicks + " cliques.", String.valueOf(clicks), key, Instant.now());
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
