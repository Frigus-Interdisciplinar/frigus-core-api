package com.frigus.coreapi.repository;

import com.frigus.coreapi.model.Discard;

public interface DiscardRepository extends BaseRepository<Discard, Integer>{
    org.springframework.data.domain.Page<Discard> findByStockProductStockGroupId(java.util.UUID groupId,org.springframework.data.domain.Pageable pageable);
}
