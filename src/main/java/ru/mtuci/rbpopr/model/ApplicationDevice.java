package ru.mtuci.rbpopr.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "device")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationDevice {

    @ManyToOne
    @JoinColumn(name = "userId")
    private ApplicationUser user;

    @Id
    @GeneratedValue
    private Long id;

    private String macAddress;

    private String name;
}
