package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.SupportRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SupportRequestRepository extends JpaRepository<SupportRequest, UUID> {
    Page<SupportRequest> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    Optional<SupportRequest> findByIdAndUserId(UUID id, UUID userId);
}
