package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends BaseRepository<Message, Integer> {
    @Query("SELECT m FROM Message m " +
           "WHERE m.conversationParticipants.id.conversationId = :conversationId " +
           "ORDER BY m.createdAt DESC")
    Page<Message> findByConversationIdOrderByCreatedAtDesc(@Param("conversationId") UUID conversationId, Pageable pageable);

    @Query("SELECT m FROM Message m " +
           "WHERE m.conversationParticipants.id.conversationId = :conversationId " +
           "ORDER BY m.createdAt DESC LIMIT 1")
    Optional<Message> findLatestMessageByConversationId(@Param("conversationId") UUID conversationId);

    @Query(value = "SELECT m.* FROM messages m WHERE m.conversation_id = :conversationId " +
            "AND to_tsvector('simple', coalesce(m.content, '')) @@ plainto_tsquery('simple', :search) " +
            "ORDER BY m.created_at DESC",
            countQuery = "SELECT count(*) FROM messages m WHERE m.conversation_id = :conversationId " +
                    "AND to_tsvector('simple', coalesce(m.content, '')) @@ plainto_tsquery('simple', :search)",
            nativeQuery = true)
    Page<Message> searchByConversationId(@Param("conversationId") UUID conversationId, @Param("search") String search, Pageable pageable);
}
