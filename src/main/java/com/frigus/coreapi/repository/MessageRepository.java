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
}
