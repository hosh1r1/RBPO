package ru.mtuci.rbpopr.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(name = "license_history")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationLicenseHistory {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "licenseId")
    private ApplicationLicense license;



    private String status;

    private Date changeDate;

    @ManyToOne
    @JoinColumn(name = "userId")
    private ApplicationUser user;

    private String description;
}

