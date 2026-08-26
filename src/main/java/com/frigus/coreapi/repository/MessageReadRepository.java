package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.MessageRead;
import com.frigus.coreapi.model.MessageReadId;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageReadRepository extends BaseRepository<MessageRead, MessageReadId> {
    Optional<MessageRead> findByIdMessageIdAndIdUserId(Integer messageId, UUID userId);
    boolean existsByIdMessageIdAndIdUserId(Integer messageId, UUID userId);
}
