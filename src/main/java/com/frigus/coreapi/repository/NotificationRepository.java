package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends BaseRepository<Notification, UUID> {
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    Optional<Notification> findByIdAndRecipientId(UUID id, UUID recipientId);

    boolean existsByDeduplicationKey(String deduplicationKey);

    Page<Notification> findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(UUID recipientId,Pageable pageable);
    long countByRecipientIdAndReadAtIsNull(UUID recipientId);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update Notification n set n.readAt=:now where n.recipient.id=:userId and n.readAt is null")
    int markAllRead(@org.springframework.data.repository.query.Param("userId") UUID userId,@org.springframework.data.repository.query.Param("now") java.time.Instant now);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value="insert into notifications(id,recipient_user_id,group_id,type,title,body,reference_id,deduplication_key,created_at) values(gen_random_uuid(),:recipient,:groupId,cast(:type as notification_type_enum),:title,:body,:referenceId,:key,CURRENT_TIMESTAMP) on conflict(deduplication_key) where deduplication_key is not null do nothing",nativeQuery=true)
    int insertIfAbsent(@org.springframework.data.repository.query.Param("recipient") UUID recipient,@org.springframework.data.repository.query.Param("groupId") UUID groupId,@org.springframework.data.repository.query.Param("type") String type,@org.springframework.data.repository.query.Param("title") String title,@org.springframework.data.repository.query.Param("body") String body,@org.springframework.data.repository.query.Param("referenceId") String referenceId,@org.springframework.data.repository.query.Param("key") String key);
}
