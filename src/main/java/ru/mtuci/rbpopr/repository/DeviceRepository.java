package ru.mtuci.rbpopr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mtuci.rbpopr.model.ApplicationDevice;
import ru.mtuci.rbpopr.model.ApplicationUser;

import java.util.Optional;

public interface DeviceRepository extends JpaRepository<ApplicationDevice, Long> {
    Optional<ApplicationDevice> findById(Long id);
    Optional<ApplicationDevice> findByUserAndMacAddressAndName(ApplicationUser user, String mac_address, String name);
    Optional<ApplicationDevice> findByIdAndUser(Long id, ApplicationUser user);
    Optional<ApplicationDevice> findTopByUserOrderByIdDesc(ApplicationUser user);
}
