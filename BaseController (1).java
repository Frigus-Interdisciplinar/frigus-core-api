package org.example.putmethod.controller;

import org.example.putmethod.repository.BaseRepository;
import org.example.putmethod.service.BaseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public abstract class BaseController<
        TModel,
        TId,
        TService extends BaseService<TModel, TId, TRepository>,
        TRepository extends BaseRepository<TModel, TId>> {

    protected final TService service;

    protected BaseController(TService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<TModel> post(@RequestBody TModel model) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.save(model));
    }

    @GetMapping
    public ResponseEntity<List<TModel>> get() {
        return ResponseEntity.ok(service.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TModel> getById(@PathVariable TId id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TModel> put(
            @PathVariable TId id,
            @RequestBody TModel model) {
        return ResponseEntity.ok(service.update(id, model));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable TId id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
