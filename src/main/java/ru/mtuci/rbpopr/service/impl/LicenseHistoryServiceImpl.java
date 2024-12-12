package ru.mtuci.rbpopr.service.impl;

import org.springframework.stereotype.Service;
import ru.mtuci.rbpopr.model.ApplicationLicense;
import ru.mtuci.rbpopr.model.ApplicationLicenseHistory;
import ru.mtuci.rbpopr.model.ApplicationUser;
import ru.mtuci.rbpopr.repository.LicenseHistoryRepository;
import ru.mtuci.rbpopr.repository.LicenseRepository;

import java.util.Date;
import java.util.Optional;

@Service
public class LicenseHistoryServiceImpl {
    private final LicenseHistoryRepository licenseHistoryRepository;
    private final LicenseRepository licenseRepository;

    public LicenseHistoryServiceImpl(LicenseHistoryRepository licenseHistoryRepository, LicenseRepository licenseRepository) {
        this.licenseHistoryRepository = licenseHistoryRepository;
        this.licenseRepository = licenseRepository;
    }

    public Optional<ApplicationLicenseHistory> getLicenseHistoryById(Long id) {
        return licenseHistoryRepository.findById(id);
    }

    public ApplicationLicenseHistory createNewRecord(String status, String description, ApplicationUser user, ApplicationLicense license){
        ApplicationLicenseHistory newHistory = new ApplicationLicenseHistory();
        newHistory.setLicense(license);
        newHistory.setStatus(status);
        newHistory.setChangeDate(new Date());
        newHistory.setDescription(description);
        newHistory.setUser(user);

        return licenseHistoryRepository.save(newHistory);
    }
}
