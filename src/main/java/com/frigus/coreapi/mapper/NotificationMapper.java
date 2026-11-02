package com.frigus.coreapi.mapper;

import com.frigus.coreapi.dto.notification.NotificationResponseDto;
import com.frigus.coreapi.model.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {
    public NotificationResponseDto toDto(Notification notification) {
        return NotificationResponseDto.builder()
                .id(notification.getId())
                .groupId(notification.getGroup() == null ? null : notification.getGroup().getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .body(notification.getBody())
                .referenceId(notification.getReferenceId())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
