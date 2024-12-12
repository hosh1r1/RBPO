package ru.mtuci.rbpopr.model;

import lombok.*;

@Data
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivationRequest {
    private String activationCode;

    private Long deviceId;

    private String mac_address;

    private String name;
}
