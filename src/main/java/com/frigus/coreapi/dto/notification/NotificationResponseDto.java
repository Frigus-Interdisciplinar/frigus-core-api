package com.frigus.coreapi.dto.notification;

import com.frigus.coreapi.enums.NotificationType;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDto {
    private UUID id;
    private UUID groupId;
    private NotificationType type;
    private String title;
    private String body;
    private String referenceId;
    private Instant readAt;
    private Instant createdAt;
}
