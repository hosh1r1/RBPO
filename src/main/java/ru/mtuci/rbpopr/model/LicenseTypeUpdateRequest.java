package ru.mtuci.rbpopr.model;

import lombok.*;

@Data
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicenseTypeUpdateRequest {
    private Long duration;
    private String description;
    private String name;
    private Long id;
}
