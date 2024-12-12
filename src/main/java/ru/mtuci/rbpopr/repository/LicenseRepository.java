package ru.mtuci.rbpopr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mtuci.rbpopr.model.ApplicationLicense;

import java.util.List;
import java.util.Optional;

public interface LicenseRepository extends JpaRepository<ApplicationLicense, Long> {
    Optional<ApplicationLicense> findById(Long id);
    Optional<ApplicationLicense> findTopByOrderByIdDesc();
    Optional<ApplicationLicense> findByCode(String code);
    Optional<ApplicationLicense> findByIdInAndCode(List<Long> ids, String code);
}
