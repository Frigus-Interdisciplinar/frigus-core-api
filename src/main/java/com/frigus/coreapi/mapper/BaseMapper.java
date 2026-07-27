package com.frigus.coreapi.mapper;

import org.springframework.stereotype.Component;

public interface BaseMapper<TModel, TResponseDto, TRequestDto> {
    TResponseDto toDto(TModel model);
    TModel toEntity(TRequestDto dto);
}