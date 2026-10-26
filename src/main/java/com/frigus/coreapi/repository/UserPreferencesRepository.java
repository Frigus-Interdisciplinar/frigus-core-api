package com.frigus.coreapi.repository;
import com.frigus.coreapi.model.*;
import java.util.*;
import java.time.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
public interface UserPreferencesRepository extends BaseRepository<UserPreferences,UUID> {

}
