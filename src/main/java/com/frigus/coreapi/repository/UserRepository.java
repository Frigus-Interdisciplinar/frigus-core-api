package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends BaseRepository<User, UUID> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
}
