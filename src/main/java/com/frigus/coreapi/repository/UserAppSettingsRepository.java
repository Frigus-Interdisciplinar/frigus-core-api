package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.UserAppSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserAppSettingsRepository extends JpaRepository<UserAppSettings, UUID> {}
