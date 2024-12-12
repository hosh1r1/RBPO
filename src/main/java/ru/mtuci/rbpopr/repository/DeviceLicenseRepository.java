package ru.mtuci.rbpopr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mtuci.rbpopr.model.ApplicationDeviceLicense;

import java.util.List;
import java.util.Optional;

public interface DeviceLicenseRepository extends JpaRepository<ApplicationDeviceLicense, Long> {
    Optional<ApplicationDeviceLicense> findById(Long id);
    List<ApplicationDeviceLicense> findByDeviceId(Long deviceId);
    Long countByLicenseId(Long licenseId);
}