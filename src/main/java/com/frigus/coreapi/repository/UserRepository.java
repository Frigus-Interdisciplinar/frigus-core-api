package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.User;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRepository extends BaseRepository<User, UUID> {
    boolean existsByEmail(String email);
}
