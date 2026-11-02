package com.frigus.coreapi.repository;
import com.frigus.coreapi.model.ReceiptDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ReceiptDraftRepository extends JpaRepository<ReceiptDraft, UUID> {
 Optional<ReceiptDraft> findByIdAndCreatedById(UUID id, UUID userId);
 Optional<ReceiptDraft> findByAccessKeyAndStockId(String accessKey, Integer stockId);
}
