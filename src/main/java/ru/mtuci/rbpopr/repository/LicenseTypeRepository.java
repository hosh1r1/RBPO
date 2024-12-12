package ru.mtuci.rbpopr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mtuci.rbpopr.model.ApplicationLicenseType;

import java.util.Optional;

public interface LicenseTypeRepository extends JpaRepository<ApplicationLicenseType, Long> {
    Optional<ApplicationLicenseType> findById(Long id);
    Optional<ApplicationLicenseType> findTopByOrderByIdDesc();
}