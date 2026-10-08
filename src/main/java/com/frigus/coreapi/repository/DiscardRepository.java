package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.Discard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;

public interface DiscardRepository extends BaseRepository<Discard, Integer>{
    @Query("select d from Discard d where exists (select ug from UserGroup ug " +
            "where ug.group.id = d.stockProduct.stock.group.id and ug.user.id = :userId)")
    Page<Discard> findAccessible(@Param("userId") UUID userId, Pageable pageable);
}
