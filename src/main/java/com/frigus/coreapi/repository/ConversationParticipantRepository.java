package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.ConversationParticipant;
import com.frigus.coreapi.model.ConversationParticipantId;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationParticipantRepository extends BaseRepository<ConversationParticipant, ConversationParticipantId> {
    Optional<ConversationParticipant> findByIdConversationIdAndIdUserId(UUID conversationId, UUID userId);

    @Query("SELECT cp FROM ConversationParticipant cp " +
           "WHERE cp.id.conversationId = :conversationId AND cp.leftAt IS NULL")
    List<ConversationParticipant> findActiveParticipantsByConversationId(@Param("conversationId") UUID conversationId);

    @Query("SELECT CASE WHEN COUNT(cp) > 0 THEN true ELSE false END FROM ConversationParticipant cp " +
           "WHERE cp.id.conversationId = :conversationId AND cp.id.userId = :userId AND cp.leftAt IS NULL")
    boolean isUserActiveParticipant(@Param("conversationId") UUID conversationId, @Param("userId") UUID userId);

    List<ConversationParticipant> findByIdConversationId(UUID conversationId);
}
