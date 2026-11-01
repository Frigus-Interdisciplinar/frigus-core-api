package com.frigus.coreapi.repository;
import com.frigus.coreapi.model.ReceiptDraftLine;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ReceiptDraftLineRepository extends JpaRepository<ReceiptDraftLine, UUID> {
 List<ReceiptDraftLine> findByReceiptIdOrderById(UUID receiptId);
 Optional<ReceiptDraftLine> findByIdAndReceiptId(UUID id, UUID receiptId);
}
