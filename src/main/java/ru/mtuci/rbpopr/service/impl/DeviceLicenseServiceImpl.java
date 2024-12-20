package ru.mtuci.rbpopr.service.impl;

import org.springframework.stereotype.Service;
import ru.mtuci.rbpopr.model.ApplicationDevice;
import ru.mtuci.rbpopr.model.ApplicationDeviceLicense;
import ru.mtuci.rbpopr.model.ApplicationLicense;
import ru.mtuci.rbpopr.repository.DeviceLicenseRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;
@Service
public class DeviceLicenseServiceImpl {
    private final DeviceLicenseRepository deviceLicenseRepository;

    public DeviceLicenseServiceImpl(DeviceLicenseRepository deviceLicenseRepository) {
        this.deviceLicenseRepository = deviceLicenseRepository;
    }

    public Optional<ApplicationDeviceLicense> getDeviceById(Long id) {
        return deviceLicenseRepository.findById(id);
    }

    public List<ApplicationDeviceLicense> getAllLicenseById(ApplicationDevice device) {
        return deviceLicenseRepository.findByDeviceId(device.getId());
    }

    public Long getDeviceCountForLicense(Long licenseId) {
        return deviceLicenseRepository.countByLicenseId(licenseId);
    }

    public ApplicationDeviceLicense createDeviceLicense(ApplicationLicense license, ApplicationDevice device) {
        ApplicationDeviceLicense newLicense = new ApplicationDeviceLicense();
        newLicense.setLicense(license);
        newLicense.setDevice(device);
        newLicense.setActivationDate(new Date());
        return deviceLicenseRepository.save(newLicense);
    }

}
