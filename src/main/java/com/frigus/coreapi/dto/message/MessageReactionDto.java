package com.frigus.coreapi.dto.message;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageReactionDto {
    private UUID userId;
    private String emoji;
}
