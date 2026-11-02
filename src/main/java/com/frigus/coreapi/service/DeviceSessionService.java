package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.user.DeviceSessionResponseDto;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.utils.ServiceUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceSessionService {
    private final GroupAccessService access;
    private final RefreshTokenService sessions;

    @Transactional(readOnly = true)
    public List<DeviceSessionResponseDto> list() {
        UUID userId = access.requireCurrentUser().getId();
        UUID currentSessionId = ServiceUtils.getCurrentSessionId();
        return sessions.listSessions(userId, currentSessionId).stream()
                .map(session -> new DeviceSessionResponseDto(
                        session.id(), session.deviceName(), session.createdAt(), session.lastActiveAt(),
                        session.current(), session.location()))
                .toList();
    }

    @Transactional
    public void revoke(UUID sessionId) {
        UUID userId = access.requireCurrentUser().getId();
        if (!sessions.revokeSession(userId, sessionId)) throw new NotFoundException();
    }
}
