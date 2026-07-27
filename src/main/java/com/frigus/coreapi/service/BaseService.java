package com.frigus.coreapi.service;

import com.frigus.coreapi.exception.NotFoundException;
import com.frigus.coreapi.mapper.BaseMapper;
import com.frigus.coreapi.repository.BaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@RequiredArgsConstructor
public abstract class BaseService<TModel, TId, TRequestDto, TReponseDto, TMapper extends BaseMapper<TModel, TReponseDto, TRequestDto>, TRepository extends BaseRepository<TModel, TId>> {
    protected final TRepository repository;
    protected final TMapper mapper;

    public Page<TReponseDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    public TReponseDto findById(TId id) {
        return mapper.toDto(repository.findById(id).orElseThrow(NotFoundException::new));
    }

    public void delete(TId id) {
        try {
            repository.deleteById(id);
        } catch (Exception e) {
            throw new NotFoundException();
        }
    }
}
