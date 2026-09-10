package com.frigus.coreapi.dto.discard;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscardResponseDto {

    private Integer id;
    private Integer stockProductId;
    private String reason;
    private Instant date;
}
