package com.frigus.coreapi.repository;
import com.frigus.coreapi.model.*;
import java.util.*;
import java.time.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
public interface PasswordResetTokenRepository extends BaseRepository<PasswordResetToken,UUID> {
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select t from PasswordResetToken t where t.tokenHash=:hash")
 Optional<PasswordResetToken> lockByHash(@Param("hash") String hash);
 @Modifying @Query("update PasswordResetToken t set t.consumedAt=:now where t.user.id=:userId and t.consumedAt is null")
 void invalidateForUser(@Param("userId") UUID userId,@Param("now") Instant now);

 Optional<PasswordResetToken> findByTokenHash(String hash);
}
