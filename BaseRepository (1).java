package org.example.putmethod.repository;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.jpa.repository.JpaRepository;

@NoRepositoryBean
public interface BaseRepository<TModel, TId>
        extends JpaRepository<TModel, TId> {
}
