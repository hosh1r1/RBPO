package ru.mtuci.rbpopr.model;

import lombok.*;

@Data
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicenseUpdateRequest {
    private Long id;
    private Long ownerId;
    private Long productId;
    private Long typeId;
    private Boolean isBlocked;
    private String description;
    private Long deviceCount;
}
