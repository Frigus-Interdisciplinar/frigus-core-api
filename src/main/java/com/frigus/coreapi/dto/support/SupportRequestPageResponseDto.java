package com.frigus.coreapi.dto.support;

import java.util.List;

public record SupportRequestPageResponseDto(
        List<SupportRequestResponseDto> content,
        long totalElements,
        int totalPages,
        int size,
        int number,
        int numberOfElements,
        boolean first,
        boolean last,
        boolean empty
) {}
