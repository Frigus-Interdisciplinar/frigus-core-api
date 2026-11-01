package com.frigus.coreapi.repository;
import com.frigus.coreapi.model.*;
import java.util.*;
import java.time.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
public interface GroupInvitationRepository extends BaseRepository<GroupInvitation,UUID> {
 List<GroupInvitation> findByGroupIdOrderByCreatedAtDesc(UUID groupId);
 Optional<GroupInvitation> findByGroupIdAndEmailIgnoreCaseAndAcceptedAtIsNullAndRevokedAtIsNull(UUID groupId,String email);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select i from GroupInvitation i where i.tokenHash=:hash")
 Optional<GroupInvitation> lockByHash(@Param("hash") String hash);

}
