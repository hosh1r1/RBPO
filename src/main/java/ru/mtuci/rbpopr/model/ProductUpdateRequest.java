package ru.mtuci.rbpopr.model;

import lombok.*;

@Data
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateRequest {
    private Long productId;
    private String name;
    private Boolean isBlocked;
}
