package com.frigus.coreapi.dto.group;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupUpdateRequestDto {
    private String name;
    private String bannerPicture;
}
