package ru.mtuci.rbpopr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mtuci.rbpopr.model.ApplicationLicenseHistory;

import java.util.Optional;

public interface LicenseHistoryRepository extends JpaRepository<ApplicationLicenseHistory, Long> {
    Optional<ApplicationLicenseHistory> findById(Long id);
}
