package com.frigus.coreapi.repository;

import com.frigus.coreapi.enums.AdvertisementStatus;
import com.frigus.coreapi.model.Advertisement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;

public interface AdvertisementRepository extends BaseRepository<Advertisement, UUID> {
    Page<Advertisement> findByAdvertiserIdOrderByUpdatedAtDesc(UUID advertiserId, Pageable pageable);

    @Query("select a from Advertisement a where a.status = :status " +
            "and (a.startDate is null or a.startDate <= :today) " +
            "and (a.endDate is null or a.endDate >= :today) order by a.createdAt desc")
    Page<Advertisement> findEligibleForDisplay(@Param("status") AdvertisementStatus status,
                                                @Param("today") LocalDate today,
                                                Pageable pageable);

    @Modifying
    @Query("update Advertisement a set a.impressions = a.impressions + 1 " +
            "where a.id = :id and a.status = :status " +
            "and (a.startDate is null or a.startDate <= :today) " +
            "and (a.endDate is null or a.endDate >= :today)")
    int recordImpression(@Param("id") UUID id, @Param("status") AdvertisementStatus status, @Param("today") LocalDate today);

    @Modifying
    @Query("update Advertisement a set a.clicks = a.clicks + 1 " +
            "where a.id = :id and a.status = :status " +
            "and (a.startDate is null or a.startDate <= :today) " +
            "and (a.endDate is null or a.endDate >= :today)")
    int recordClick(@Param("id") UUID id, @Param("status") AdvertisementStatus status, @Param("today") LocalDate today);
}
