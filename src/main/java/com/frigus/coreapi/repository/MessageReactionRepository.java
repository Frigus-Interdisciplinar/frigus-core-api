package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.MessageReaction;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageReactionRepository extends BaseRepository<MessageReaction, Integer> {
    List<MessageReaction> findByMessage_IdOrderByCreatedAtAsc(Integer messageId);
    Optional<MessageReaction> findByMessage_IdAndUser_IdAndEmoji(Integer messageId, UUID userId, String emoji);
}
