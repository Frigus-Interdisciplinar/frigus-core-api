package com.frigus.coreapi.mapper;

public interface BaseMapper<TModel, TResponseDto> {
    TResponseDto toDto(TModel model);
    TModel toEntity(TResponseDto dto);
}