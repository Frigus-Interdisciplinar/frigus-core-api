package com.frigus.coreapi.service;
import com.frigus.coreapi.dto.user.*;
import com.frigus.coreapi.exception.BadRequestException;
import com.frigus.coreapi.model.*;
import com.frigus.coreapi.repository.*;
import com.frigus.coreapi.utils.OneTimeTokens;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
@ExtendWith(MockitoExtension.class)
class PasswordRecoveryServiceTest {
 @Mock PasswordResetTokenRepository tokens;
 @Mock UserRepository users;
 @Mock PasswordEncoder encoder;
 @Mock EmailService email;
 @Mock StringRedisTemplate redis;
 @Mock ValueOperations<String,String> values;
 @InjectMocks PasswordRecoveryService service;
 User user;
 @BeforeEach void setup(){user=User.builder().id(UUID.randomUUID()).email("ana@example.test").hashPassword("old").build();}
 @Test void recoveryStoresOnlyHashAndSendsOneTimeLink(){
  when(redis.opsForValue()).thenReturn(values);when(values.setIfAbsent(anyString(),eq("1"),any(Duration.class))).thenReturn(true);
  when(users.findByEmailIgnoreCaseAndDeletedAtIsNull(user.getEmail())).thenReturn(Optional.of(user));when(users.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
  service.request(new ForgotPasswordRequestDto("ANA@example.test"));
  ArgumentCaptor<PasswordResetToken> stored=ArgumentCaptor.forClass(PasswordResetToken.class);ArgumentCaptor<String> raw=ArgumentCaptor.forClass(String.class);
  verify(tokens).save(stored.capture());verify(email).sendLink(eq(user.getEmail()),anyString(),eq("/reset-password"),raw.capture());
  assertThat(stored.getValue().getTokenHash()).isEqualTo(OneTimeTokens.hash(raw.getValue())).isNotEqualTo(raw.getValue());
  assertThat(stored.getValue().getExpiresAt()).isAfter(Instant.now().plusSeconds(1700));
 }
 @Test void resetRevokesPreviouslyIssuedSessionsAndConsumesAllOutstandingTokens(){
  var t=PasswordResetToken.builder().user(user).tokenHash(OneTimeTokens.hash("token")).expiresAt(Instant.now().plusSeconds(60)).build();
  when(tokens.findByTokenHash(t.getTokenHash())).thenReturn(Optional.of(t));when(tokens.lockByHash(t.getTokenHash())).thenReturn(Optional.of(t));when(users.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));when(encoder.encode("NewPass123!")).thenReturn("new-hash");
  service.reset(new ResetPasswordRequestDto("token","NewPass123!"));
  assertThat(user.getHashPassword()).isEqualTo("new-hash");assertThat(user.getTokenVersion()).isEqualTo(1);verify(tokens).invalidateForUser(eq(user.getId()),any(Instant.class));
 }
 @Test void expiredTokenCannotChangePassword(){
  var t=PasswordResetToken.builder().user(user).tokenHash(OneTimeTokens.hash("token")).expiresAt(Instant.now().minusSeconds(1)).build();
  when(tokens.findByTokenHash(t.getTokenHash())).thenReturn(Optional.of(t));when(tokens.lockByHash(t.getTokenHash())).thenReturn(Optional.of(t));when(users.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
  assertThatThrownBy(() -> service.reset(new ResetPasswordRequestDto("token","NewPass123!"))).isInstanceOf(BadRequestException.class);verifyNoInteractions(encoder);verify(users,never()).save(any());
 }
 @Test void repeatedRecoveryRequestDoesNotSendMoreMail(){
  when(redis.opsForValue()).thenReturn(values);when(values.setIfAbsent(anyString(),eq("1"),any(Duration.class))).thenReturn(false);
  service.request(new ForgotPasswordRequestDto(user.getEmail()));verifyNoInteractions(users,tokens,email);
 }
}
