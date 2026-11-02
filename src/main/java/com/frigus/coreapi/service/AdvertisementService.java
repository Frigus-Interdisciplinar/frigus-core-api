package com.frigus.coreapi.service;

import com.frigus.coreapi.dto.advertisement.AdvertisementRequestDto;
import com.frigus.coreapi.dto.advertisement.AdvertisementResponseDto;
import com.frigus.coreapi.enums.AdvertisementStatus;
import com.frigus.coreapi.enums.AccountType;
import com.frigus.coreapi.enums.Role;
import com.frigus.coreapi.exception.BadRequestException;
import com.frigus.coreapi.exception.ConflictException;
import com.frigus.coreapi.exception.ForbiddenException;
import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.model.Advertisement;
import com.frigus.coreapi.model.User;
import com.frigus.coreapi.repository.AdvertisementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdvertisementService {
    private final AdvertisementRepository advertisements;
    private final GroupAccessService access;

    public Page<AdvertisementResponseDto> listMine(Pageable pageable) {
        User user = requireAdvertiser();
        return advertisements.findByAdvertiserIdOrderByUpdatedAtDesc(user.getId(), pageable).map(this::toDto);
    }

    public Page<AdvertisementResponseDto> listActive(Pageable pageable) {
        return advertisements.findEligibleForDisplay(AdvertisementStatus.ACTIVE, LocalDate.now(), pageable).map(this::toDto);
    }

    @Transactional
    public AdvertisementResponseDto create(AdvertisementRequestDto request) {
        User user = requireAdvertiser();
        validateDates(request);
        Advertisement advertisement = Advertisement.builder().advertiser(user).build();
        apply(advertisement, request);
        return toDto(advertisements.save(advertisement));
    }

    @Transactional
    public AdvertisementResponseDto update(UUID id, AdvertisementRequestDto request) {
        Advertisement advertisement = requireOwned(id);
        if (advertisement.getStatus() == AdvertisementStatus.ARCHIVED) {
            throw new ConflictException("Anúncio arquivado", "Anúncios arquivados não podem ser editados");
        }
        validateDates(request);
        apply(advertisement, request);
        return toDto(advertisements.save(advertisement));
    }

    @Transactional
    public AdvertisementResponseDto changeStatus(UUID id, AdvertisementStatus next) {
        Advertisement advertisement = requireOwned(id);
        AdvertisementStatus current = advertisement.getStatus();
        boolean allowed = switch (current) {
            case DRAFT -> next == AdvertisementStatus.ACTIVE || next == AdvertisementStatus.ARCHIVED;
            case ACTIVE -> next == AdvertisementStatus.PAUSED || next == AdvertisementStatus.ARCHIVED;
            case PAUSED -> next == AdvertisementStatus.ACTIVE || next == AdvertisementStatus.ARCHIVED;
            case ARCHIVED -> false;
        };
        if (!allowed) throw new ConflictException("Transição de anúncio inválida", "Não é possível alterar o anúncio para este estado");
        if (next == AdvertisementStatus.ACTIVE) validateCanActivate(advertisement);
        advertisement.setStatus(next);
        advertisement.setUpdatedAt(Instant.now());
        return toDto(advertisements.save(advertisement));
    }

    @Transactional
    public void recordImpression(UUID id) {
        if (advertisements.recordImpression(id, AdvertisementStatus.ACTIVE, LocalDate.now()) == 0) throw new NotFoundException();
    }

    @Transactional
    public void recordClick(UUID id) {
        if (advertisements.recordClick(id, AdvertisementStatus.ACTIVE, LocalDate.now()) == 0) throw new NotFoundException();
    }

    private User requireAdvertiser() {
        User user = access.requireCurrentUser();
        if (user.getRole() != Role.ADMIN && user.getAccountType() != AccountType.BUSINESS) {
            throw new ForbiddenException("Conta empresarial necessária", "A gestão de anúncios está disponível para contas empresariais");
        }
        return user;
    }

    private Advertisement requireOwned(UUID id) {
        User user = requireAdvertiser();
        Advertisement advertisement = advertisements.findById(id).orElseThrow(NotFoundException::new);
        if (user.getRole() != Role.ADMIN && !advertisement.getAdvertiser().getId().equals(user.getId())) {
            throw new NotFoundException();
        }
        return advertisement;
    }

    private void apply(Advertisement advertisement, AdvertisementRequestDto request) {
        advertisement.setName(request.name().trim());
        advertisement.setProductName(request.productName().trim());
        advertisement.setCategory(request.category().trim());
        advertisement.setDescription(request.description().trim());
        advertisement.setImageUrl(request.imageUrl().trim());
        advertisement.setDestinationUrl(request.destinationUrl() == null || request.destinationUrl().isBlank() ? null : request.destinationUrl().trim());
        advertisement.setPrice(request.price());
        advertisement.setStartDate(request.startDate());
        advertisement.setEndDate(request.endDate());
        advertisement.setUpdatedAt(Instant.now());
        if (advertisement.getStatus() == AdvertisementStatus.ACTIVE) validateCanActivate(advertisement);
    }

    private void validateDates(AdvertisementRequestDto request) {
        if (request.startDate() != null && request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new BadRequestException("Período inválido", "A data final precisa ser igual ou posterior à data inicial");
        }
    }

    private void validateCanActivate(Advertisement advertisement) {
        if (advertisement.getImageUrl() == null || advertisement.getImageUrl().isBlank()) {
            throw new BadRequestException("Imagem obrigatória", "Informe uma imagem pública HTTPS antes de publicar o anúncio");
        }
        if (advertisement.getEndDate() != null && advertisement.getEndDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Período encerrado", "Atualize a data final antes de publicar o anúncio");
        }
    }

    private AdvertisementResponseDto toDto(Advertisement advertisement) {
        return new AdvertisementResponseDto(
                advertisement.getId(), advertisement.getAdvertiser().getId(), advertisement.getAdvertiser().getName(),
                advertisement.getName(), advertisement.getProductName(), advertisement.getCategory(), advertisement.getDescription(),
                advertisement.getImageUrl(), advertisement.getDestinationUrl(), advertisement.getPrice(), advertisement.getStartDate(),
                advertisement.getEndDate(), advertisement.getStatus(), advertisement.getImpressions(), advertisement.getClicks(),
                advertisement.getCreatedAt(), advertisement.getUpdatedAt());
    }
}
