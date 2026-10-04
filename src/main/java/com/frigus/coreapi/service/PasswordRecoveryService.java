package com.frigus.coreapi.service;
import com.frigus.coreapi.dto.user.*;
import com.frigus.coreapi.model.*;
import com.frigus.coreapi.repository.*;
import com.frigus.coreapi.exception.BadRequestException;
import com.frigus.coreapi.utils.OneTimeTokens;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mail.MailException;
import java.time.*;
import java.util.Locale;
@Service @RequiredArgsConstructor @Slf4j
public class PasswordRecoveryService {
 private final PasswordResetTokenRepository tokens;
 private final UserRepository users;
 private final PasswordEncoder encoder;
 private final EmailService email;
 private final StringRedisTemplate redis;
 @Transactional public void request(ForgotPasswordRequestDto dto){
  String address=dto.email().trim().toLowerCase(Locale.ROOT);
  if(!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent("password-request:"+OneTimeTokens.hash(address),"1",Duration.ofMinutes(1)))) return;
  var found=users.findByEmailIgnoreCaseAndDeletedAtIsNull(address);
  if(found.isEmpty()) return;
  User user=users.findByIdForUpdate(found.get().getId()).orElseThrow();
  tokens.invalidateForUser(user.getId(),Instant.now());
  String raw=OneTimeTokens.create();
  tokens.save(PasswordResetToken.builder().user(user).tokenHash(OneTimeTokens.hash(raw)).expiresAt(Instant.now().plus(Duration.ofMinutes(30))).build());
  try{email.sendLink(address,"Redefinição de senha Frigus","/reset-password",raw);}
  catch(MailException e){log.warn("Falha no provedor de e-mail de recuperação: {}",e.getClass().getSimpleName());}
 }
 @Transactional public void reset(ResetPasswordRequestDto dto){
  String hash=OneTimeTokens.hash(dto.token());
  var reference=tokens.findByTokenHash(hash).orElseThrow(this::invalid);
  User user=users.findByIdForUpdate(reference.getUser().getId()).orElseThrow(this::invalid);
  var t=tokens.lockByHash(hash).orElseThrow(this::invalid);
  if(t.getConsumedAt()!=null || !t.getExpiresAt().isAfter(Instant.now())) throw invalid();
  if(user.getDeletedAt()!=null) throw invalid();
  user.setHashPassword(encoder.encode(dto.newPassword()));user.setTokenVersion(user.getTokenVersion()+1);users.save(user);
  tokens.invalidateForUser(user.getId(),Instant.now());
 }
 private BadRequestException invalid(){return new BadRequestException("Token inválido ou expirado","Solicite um novo link de recuperação");}
}
