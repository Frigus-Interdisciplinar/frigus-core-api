package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.MessageAttachment;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageAttachmentRepository extends BaseRepository<MessageAttachment, Integer> {
    List<MessageAttachment> findByMessage_IdOrderByIdAsc(Integer messageId);

    @Query("SELECT attachment FROM MessageAttachment attachment " +
            "WHERE attachment.message.id IN :messageIds ORDER BY attachment.message.id, attachment.id")
    List<MessageAttachment> findByMessageIdsOrderByMessageAndId(@Param("messageIds") List<Integer> messageIds);
}
