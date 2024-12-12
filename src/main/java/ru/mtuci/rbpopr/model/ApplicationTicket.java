package ru.mtuci.rbpopr.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ApplicationTicket {
    private Date currentDate;
    private Long deviceId;
    private Date lifetime;
    private Date activationDate;
    private Date expirationDate;
    private Long userId;
    private String digitalSignature;
    private String info;
    private String status;
    private boolean licenseBlocked;
}
