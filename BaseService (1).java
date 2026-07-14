package org.example.putmethod.service;

import org.example.putmethod.repository.BaseRepository;

import java.util.List;

public abstract class BaseService<
        TModel,
        TId,
        TRepository extends BaseRepository<TModel, TId>> {

    protected final TRepository repository;

    protected BaseService(TRepository repository) {
        this.repository = repository;
    }

    public TModel save(TModel model) {
        return repository.save(model);
    }

    public List<TModel> list() {
        return repository.findAll();
    }

    public TModel findById(TId id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registro não encontrado"));
    }

    public TModel update(TId id, TModel model) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Registro não encontrado");
        }

        return repository.save(model);
    }

    public void delete(TId id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Registro não encontrado");
        }

        repository.deleteById(id);
    }
}
