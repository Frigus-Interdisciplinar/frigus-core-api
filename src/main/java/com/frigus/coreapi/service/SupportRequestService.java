package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.support.SupportRequestCreateDto;
import com.frigus.coreapi.dto.support.SupportRequestPageResponseDto;
import com.frigus.coreapi.dto.support.SupportRequestResponseDto;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.model.SupportRequest;
import com.frigus.coreapi.repository.SupportRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupportRequestService {
    private final SupportRequestRepository repository;
    private final GroupAccessService access;

    @Transactional
    public SupportRequestResponseDto create(SupportRequestCreateDto request) {
        var userId = access.requireCurrentUser().getId();
        return toDto(repository.save(SupportRequest.builder()
                .userId(userId)
                .subject(request.subject().trim())
                .description(request.description().trim())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build()));
    }

    @Transactional(readOnly = true)
    public SupportRequestPageResponseDto list(int page, int size) {
        UUID userId = access.requireCurrentUser().getId();
        Page<SupportRequestResponseDto> result = repository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size)).map(this::toDto);
        return new SupportRequestPageResponseDto(
                result.getContent(), result.getTotalElements(), result.getTotalPages(), result.getSize(),
                result.getNumber(), result.getNumberOfElements(), result.isFirst(), result.isLast(), result.isEmpty());
    }

    @Transactional(readOnly = true)
    public SupportRequestResponseDto get(UUID id) {
        UUID userId = access.requireCurrentUser().getId();
        return repository.findByIdAndUserId(id, userId).map(this::toDto).orElseThrow(NotFoundException::new);
    }

    private SupportRequestResponseDto toDto(SupportRequest request) {
        return new SupportRequestResponseDto(request.getId(), request.getProtocol(), request.getStatus(),
                request.getSubject(), request.getDescription(), request.getCreatedAt(), request.getUpdatedAt());
    }
}
