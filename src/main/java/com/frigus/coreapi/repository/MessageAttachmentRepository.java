package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.MessageAttachment;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageAttachmentRepository extends BaseRepository<MessageAttachment, Integer> {
    List<MessageAttachment> findByMessage_IdOrderByIdAsc(Integer messageId);
}
