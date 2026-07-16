package com.frigus.coreapi.repository;

import com.frigus.coreapi.exception.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BaseRepository<TModel, TId> extends JpaRepository<TModel, TId> { }
