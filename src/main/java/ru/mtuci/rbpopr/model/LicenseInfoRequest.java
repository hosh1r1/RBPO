package ru.mtuci.rbpopr.model;

import lombok.*;

@Data
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicenseInfoRequest {
    private String name;
    private String mac_address;
    private String activationCode;
}
