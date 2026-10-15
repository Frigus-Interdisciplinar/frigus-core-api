package com.frigus.coreapi.controller;

import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.frigus.coreapi.dto.discard.DiscardCreateRequestDto;
import com.frigus.coreapi.dto.discard.DiscardResponseDto;
import com.frigus.coreapi.model.Discard;
import com.frigus.coreapi.service.DiscardService;

import java.util.UUID;

@RestController
@RequestMapping("/discard")
public class DiscardController extends BaseController<Discard, Integer, DiscardCreateRequestDto, DiscardResponseDto, DiscardService> {

    public DiscardController(DiscardService service) {
        super(service);
    }

    @GetMapping
    public Page<DiscardResponseDto> findAll(
            @RequestParam UUID groupId,
            @ParameterObject @PageableDefault(size = 20, sort = {"date", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return service.findByGroupId(groupId, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiscardResponseDto create(@Valid @RequestBody DiscardCreateRequestDto dto) {
        return service.create(dto);
    }
}
