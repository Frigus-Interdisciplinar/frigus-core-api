package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.notification.AdClickReportRequestDto;
import com.frigus.coreapi.dto.notification.NotificationResponseDto;
import com.frigus.coreapi.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "Notificações do usuário autenticado")
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Listar minhas notificações")
    public Page<NotificationResponseDto> list(Pageable pageable) {
        return notificationService.listMyNotifications(pageable);
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Marcar uma notificação como lida")
    public NotificationResponseDto markAsRead(@PathVariable UUID id) {
        return notificationService.markAsRead(id);
    }

    @PostMapping("/ad-clicks")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Registrar o total de cliques para avisos empresariais a cada 2500 cliques")
    public void reportAdClicks(@Valid @RequestBody AdClickReportRequestDto dto,
                               @RequestHeader("X-Ad-Clicks-Timestamp") long timestamp,
                               @RequestHeader("X-Ad-Clicks-Signature") String signature) {
        notificationService.notifyAdClickMilestones(dto.getTotalClicks(), timestamp, signature);
    }
}
