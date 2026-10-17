package com.frigus.coreapi.dto.message;

import lombok.*;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageAttachmentResponseDto {
    private Integer id;
    private String fileUrl;
    private String fileName;
    private String mimeType;
    private Integer fileSize;
    private Instant createdAt;
}
