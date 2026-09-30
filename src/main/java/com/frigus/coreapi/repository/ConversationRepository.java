package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.Conversation;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends BaseRepository<Conversation, UUID> {
    Optional<Conversation> findByPairKey(String pairKey);

    @Query(value = "SELECT * FROM conversations WHERE pair_key = fn_pair_key(:userA, :userB)", nativeQuery = true)
    Optional<Conversation> findPrivateConversationByUsers(@Param("userA") UUID userA, @Param("userB") UUID userB);

    @Query(value = "SELECT fn_pair_key(:userA, :userB)", nativeQuery = true)
    String getPairKeyFromDb(@Param("userA") UUID userA, @Param("userB") UUID userB);

    @Query("SELECT cp.conversation FROM ConversationParticipant cp " +
           "WHERE cp.user.id = :userId AND cp.leftAt IS NULL " +
           "ORDER BY cp.conversation.updatedAt DESC")
    List<Conversation> findActiveConversationsByUserId(@Param("userId") UUID userId);

    @Query("SELECT c FROM Conversation c " +
           "WHERE c.group.id = :groupId")
    List<Conversation> findByGroupId(@Param("groupId") UUID groupId);
}
