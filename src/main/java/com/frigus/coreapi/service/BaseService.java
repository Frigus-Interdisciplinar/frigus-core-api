package com.frigus.coreapi.service;

import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.mapper.BaseMapper;
import com.frigus.coreapi.repository.BaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

@RequiredArgsConstructor
public abstract class BaseService<TModel, TId, TRequestDto, TResponseDto, TMapper extends BaseMapper<TModel, TResponseDto, TRequestDto>, TRepository extends BaseRepository<TModel, TId>> {
    protected final TRepository repository;
    protected final TMapper mapper;

    @Transactional(readOnly = true)
    public Page<TResponseDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::mapToDto);
    }

    public Page<TResponseDto> findAll(Pageable pageable, MultiValueMap<String, String> filters) {
        return findAll(pageable);
    }

    @Transactional(readOnly = true)
    public TResponseDto findById(TId id) {
        TModel entity = getRequiredEntity(id);
        authorizeRead(entity);
        return mapToDto(entity);
    }

    @Transactional
    public void delete(TId id) {
        if (!requiresAuthorizedDelete()) {
            try {
                repository.deleteById(id);
                return;
            } catch (Exception exception) {
                throw new NotFoundException();
            }
        }

        TModel entity = getRequiredEntity(id);
        authorizeDelete(entity);
        repository.delete(entity);
    }

    protected TModel getRequiredEntity(TId id) {
        return repository.findById(id).orElseThrow(NotFoundException::new);
    }

    protected void authorizeRead(TModel entity) {
        // Resources without scoped authorization keep their existing behavior.
    }

    protected void authorizeDelete(TModel entity) {
        authorizeRead(entity);
    }

    protected boolean requiresAuthorizedDelete() {
        return false;
    }

    protected TResponseDto mapToDto(TModel entity) {
        return mapper.toDto(entity);
    }
}
