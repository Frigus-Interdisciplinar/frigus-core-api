package com.frigus.coreapi.controller;

import com.frigus.coreapi.dto.notification.NotificationResponseDto;
import com.frigus.coreapi.enums.NotificationType;
import com.frigus.coreapi.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {
    @Mock
    private NotificationService notificationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(notificationService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void listsNotificationsThroughHttpEndpoint() throws Exception {
        NotificationResponseDto notification = NotificationResponseDto.builder()
                .id(UUID.randomUUID())
                .type(NotificationType.PRODUCT_NEAR_EXPIRATION)
                .title("Produto próximo da validade")
                .build();
        when(notificationService.listMyNotifications(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(notification), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/notifications").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("PRODUCT_NEAR_EXPIRATION"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void marksNotificationAsReadThroughHttpEndpoint() throws Exception {
        UUID id = UUID.randomUUID();
        when(notificationService.markAsRead(id)).thenReturn(NotificationResponseDto.builder()
                .id(id)
                .type(NotificationType.GROUP_MEMBER_JOINED)
                .title("Novo membro no grupo")
                .build());

        mockMvc.perform(patch("/notifications/{id}/read", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.type").value("GROUP_MEMBER_JOINED"));
    }

    @Test
    void reportsPositiveAdClickTotalThroughHttpEndpoint() throws Exception {
        mockMvc.perform(post("/notifications/ad-clicks")
                        .contentType("application/json")
                        .content("{\"totalClicks\":5000}"))
                .andExpect(status().isNoContent());

        verify(notificationService).notifyAdClickMilestones(5000L);
    }

    @Test
    void rejectsMissingOrNonPositiveAdClickTotal() throws Exception {
        mockMvc.perform(post("/notifications/ad-clicks")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/notifications/ad-clicks")
                        .contentType("application/json")
                        .content("{\"totalClicks\":0}"))
                .andExpect(status().isBadRequest());
    }
}
