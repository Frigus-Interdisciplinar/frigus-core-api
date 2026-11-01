package com.frigus.coreapi.dto.message;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageAttachmentInputDto {
    private String fileUrl;
    private String fileName;
    private String mimeType;
    private Integer fileSize;
}
