package com.frigus.coreapi.mapper;

import org.springframework.stereotype.Component;

import com.frigus.coreapi.dto.discard.DiscardCreateRequestDto;
import com.frigus.coreapi.dto.discard.DiscardResponseDto;
import com.frigus.coreapi.model.Discard;

@Component
public class DiscardMapper implements BaseMapper<Discard, DiscardResponseDto, DiscardCreateRequestDto> {
    @Override
    public DiscardResponseDto toDto(Discard discard) {
        return DiscardResponseDto.builder()
                .id(discard.getId())
                .stockProductId(discard.getStockProduct().getId())
                .reason(discard.getReason())
                .date(discard.getDate())
                .build();
    }

    @Override
    public Discard toEntity(DiscardCreateRequestDto dto) {
        return Discard.builder()
                .reason(dto.getReason())
                .build();
    }
}
