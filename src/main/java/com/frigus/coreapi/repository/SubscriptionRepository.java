package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.Subscription;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends BaseRepository<Subscription, UUID> {
    Optional<Subscription> findByUserId(UUID userId);
}
