package com.frigus.coreapi.service;

import com.frigus.coreapi.enums.AccountType;
import com.frigus.coreapi.exception.BadRequestException;
import com.frigus.coreapi.exception.ForbiddenException;
import com.frigus.coreapi.mapper.NotificationMapper;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.NotificationRepository;
import com.frigus.coreapi.repository.UserGroupRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationAdClickSecurityTest {
    @Mock NotificationRepository notifications;
    @Mock UserGroupRepository memberships;
    @Mock NotificationMapper mapper;
    @Mock JdbcTemplate jdbc;
    @InjectMocks NotificationService service;
    User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).accountType(AccountType.BUSINESS).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
        ReflectionTestUtils.setField(service, "reportSecret", "test-report-secret");
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsExtremeReportBeforeQueryingDatabase() throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        long clicks = 1_000_000_001L;
        assertThatThrownBy(() -> service.notifyAdClickMilestones(clicks, timestamp, sign(clicks, timestamp)))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(jdbc);
    }

    @Test
    void rejectsUnsignedReportBeforeQueryingDatabase() {
        long timestamp = Instant.now().getEpochSecond();
        assertThatThrownBy(() -> service.notifyAdClickMilestones(2500L, timestamp, "00"))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(jdbc);
    }

    @Test
    void insertsOnlyNewMilestoneAndRejectsExcessiveAdvance() throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        when(jdbc.queryForObject(anyString(), eq(Long.class), eq(user.getId())))
                .thenReturn(0L, 2500L, 2500L);

        service.notifyAdClickMilestones(2500L, timestamp, sign(2500L, timestamp));
        service.notifyAdClickMilestones(2500L, timestamp, sign(2500L, timestamp));
        assertThatThrownBy(() -> service.notifyAdClickMilestones(15000L, timestamp, sign(15000L, timestamp)))
                .isInstanceOf(BadRequestException.class);
        verify(jdbc, times(1)).update(anyString(), any(), any(), any(), any(), any(), any(), any());
    }

    private String sign(long clicks, long timestamp) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("test-report-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(
                (user.getId() + ":" + clicks + ":" + timestamp).getBytes(StandardCharsets.UTF_8)));
    }
}
