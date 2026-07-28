package com.frigus.coreapi.mapper;

public interface BaseMapper<TModel, TResponseDto, TRequestDto> {
    TResponseDto toDto(TModel model);
    TModel toEntity(TRequestDto dto);
}