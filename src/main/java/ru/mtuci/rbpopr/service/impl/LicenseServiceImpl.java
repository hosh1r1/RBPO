package ru.mtuci.rbpopr.service.impl;

import org.springframework.stereotype.Service;
import ru.mtuci.rbpopr.model.*;
import ru.mtuci.rbpopr.repository.LicenseRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.security.*;
import java.util.stream.Collectors;

@Service
public class LicenseServiceImpl {
    private final LicenseRepository licenseRepository;
    private final LicenseTypeServiceImpl licenseTypeService;
    private final ProductServiceImpl productService;
    private final DeviceLicenseServiceImpl deviceLicenseService;
    private final LicenseHistoryServiceImpl licenseHistoryService;
    private final UserDetailsServiceImpl userDetailsServiceImpl;
    private final DeviceServiceImpl deviceServiceImpl;

    public LicenseServiceImpl(LicenseRepository licenseRepository, LicenseTypeServiceImpl licenseTypeService,
                              ProductServiceImpl productService, DeviceLicenseServiceImpl deviceLicenseService,
                              LicenseHistoryServiceImpl licenseHistoryService, UserDetailsServiceImpl userDetailsServiceImpl, DeviceServiceImpl deviceServiceImpl) {
        this.licenseRepository = licenseRepository;
        this.licenseTypeService = licenseTypeService;
        this.productService = productService;
        this.deviceLicenseService = deviceLicenseService;
        this.licenseHistoryService = licenseHistoryService;
        this.userDetailsServiceImpl = userDetailsServiceImpl;
        this.deviceServiceImpl = deviceServiceImpl;
    }

    public Optional<ApplicationLicense> getLicenseById(Long id) {
        return licenseRepository.findById(id);
    }

    public Long createLicense(Long productId, Long ownerId, Long licenseTypeId, ApplicationUser user, Long count) {
        ApplicationLicenseType licenseType = licenseTypeService.getLicenseTypeById(licenseTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid license type ID"));

        ApplicationProduct product = productService.getProductById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid product ID"));

        ApplicationLicense license = new ApplicationLicense();

        String uuid;
        do {
            uuid = UUID.randomUUID().toString();
        } while (licenseRepository.findByCode(uuid).isPresent());

        license.setCode(uuid);
        license.setProduct(product);
        license.setLicenseType(licenseType);
        license.setBlocked(product.isBlocked());
        license.setDeviceCount(count);
        license.setOwnerId(userDetailsServiceImpl.getUserById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid owner ID")));
        license.setDuration(licenseType.getDefaultDuration());
        license.setDescription(licenseType.getDescription());

        licenseRepository.save(license);

        licenseHistoryService.createNewRecord("Not activated", "Created new license", user, license);

        return license.getId();
    }


    public ApplicationTicket getActiveLicensesForDevice(ApplicationDevice device, String code) {
        List<ApplicationDeviceLicense> applicationDeviceLicensesList = deviceLicenseService.getAllLicenseById(device);

        List<Long> licenseIds = applicationDeviceLicensesList.stream()
                .map(license -> license.getLicense() != null ? license.getLicense().getId() : null)
                .filter(Objects::nonNull) // Убираем null значения
                .collect(Collectors.toList());

        Optional<ApplicationLicense> applicationLicense = licenseRepository.findByIdInAndCode(licenseIds, code);

        ApplicationTicket ticket = new ApplicationTicket();

        if (applicationLicense.isEmpty()) {
            ticket.setInfo("License was not found");
            ticket.setStatus("Error");
            return ticket;
        }

        ticket = createTicket(applicationLicense.get().getUser(), device, applicationLicense.get(),
                "Info about license", "OK");

        return ticket;
    }



    private String makeSignature(ApplicationTicket ticket) {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            PrivateKey privateKey = keyPair.getPrivate();

            ObjectMapper objectMapper = new ObjectMapper();
            String ticketJson = objectMapper.writeValueAsString(ticket);

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(ticketJson.getBytes());

            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            return String.format("Error generating signature: %s", e.getMessage());
        }
    }

    public ApplicationTicket createTicket(ApplicationUser user, ApplicationDevice device,
                                          ApplicationLicense license, String info, String status) {
        ApplicationTicket ticket = new ApplicationTicket();
        ticket.setCurrentDate(new Date());

        if (user != null) {
            ticket.setUserId(user.getId());
        }

        if (device != null) {
            ticket.setDeviceId(device.getId());
        }

        Calendar lifetimeCalendar = Calendar.getInstance();
        lifetimeCalendar.setTime(new Date());
        lifetimeCalendar.add(Calendar.HOUR, 1);
        ticket.setLifetime(lifetimeCalendar.getTime());

        if (license != null) {
            ticket.setActivationDate(license.getFirstActivationDate());
            ticket.setExpirationDate(license.getEndingDate());
            ticket.setLicenseBlocked(license.isBlocked());
        }

        ticket.setInfo(info);
        ticket.setDigitalSignature(makeSignature(ticket));
        ticket.setStatus(status);

        return ticket;
    }

    public ApplicationTicket activateLicense(String code, ApplicationDevice device, ApplicationUser user) {
        ApplicationTicket ticket = new ApplicationTicket();
        Optional<ApplicationLicense> optionalLicense = licenseRepository.findByCode(code);

        if (!optionalLicense.isPresent()) {
            ticket.setInfo("License not found");
            ticket.setStatus("Error");
            deviceServiceImpl.deleteLastDevice(user);
            return ticket;
        }

        ApplicationLicense existingLicense = optionalLicense.get();

        boolean isBlocked = existingLicense.isBlocked();
        boolean hasExpired = existingLicense.getEndingDate() != null && new Date().after(existingLicense.getEndingDate());
        boolean isDifferentUser = existingLicense.getUser() != null && !Objects.equals(existingLicense.getUser().getId(), user.getId());
        boolean isDeviceLimitReached = deviceLicenseService.getDeviceCountForLicense(existingLicense.getId()) >= existingLicense.getDeviceCount();

        if (isBlocked || hasExpired || isDifferentUser || isDeviceLimitReached) {
            ticket.setInfo("Activation is not possible");
            ticket.setStatus("Error");
            deviceServiceImpl.deleteLastDevice(user);
            return ticket;
        }

        if (existingLicense.getFirstActivationDate() == null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_MONTH, Math.toIntExact(existingLicense.getDuration()));
            existingLicense.setEndingDate(calendar.getTime());
            existingLicense.setFirstActivationDate(new Date());
            existingLicense.setUser(user);
        }

        deviceLicenseService.createDeviceLicense(existingLicense, device);
        licenseRepository.save(existingLicense);
        licenseHistoryService.createNewRecord("Activated", "License is valid", user, existingLicense);

        ticket = createTicket(user, device, existingLicense, "License successfully activated", "OK");

        return ticket;
    }



    public String updateLicense(Long id, Long ownerId, Long productId, Long typeId, Boolean isBlocked,
                                String description, Long deviceCount) {
        Optional<ApplicationLicense> optionalLicense = getLicenseById(id);

        if (!optionalLicense.isPresent()) {
            return "The specified license could not be found";
        }

        ApplicationLicense existingLicense = optionalLicense.get();

        if (productService.getProductById(productId).isEmpty()) {
            return "The specified product does not exist";
        }
        existingLicense.setProduct(productService.getProductById(productId).get());

        if (licenseTypeService.getLicenseTypeById(typeId).isEmpty()) {
            return "The specified license type could not be found";
        }
        existingLicense.setLicenseType(licenseTypeService.getLicenseTypeById(typeId).get());

        existingLicense.setDuration(licenseTypeService.getLicenseTypeById(typeId).get().getDefaultDuration());

        existingLicense.setBlocked(isBlocked);
        existingLicense.setOwnerId(userDetailsServiceImpl.getUserById(ownerId).orElse(null));  
        existingLicense.setDescription(description);
        existingLicense.setDeviceCount(deviceCount);

        licenseRepository.save(existingLicense);

        return "License updated successfully";
    }


    public ApplicationTicket renewalLicense(String code, ApplicationUser user) {
        ApplicationTicket ticket = new ApplicationTicket();
        Optional<ApplicationLicense> license = licenseRepository.findByCode(code);

        if (!license.isPresent()) {
            ticket.setInfo("This license key is invalid");
            ticket.setStatus("Error");
            return ticket;
        }

        ApplicationLicense newLicense = license.get();

        boolean isBlocked = newLicense.isBlocked();
        boolean hasExpired = newLicense.getEndingDate() != null && new Date().after(newLicense.getEndingDate());
        boolean isOwner = Objects.equals(newLicense.getOwnerId().getId(), user.getId());
        boolean isActivated = newLicense.getFirstActivationDate() != null;

        if (isBlocked || hasExpired || !isOwner || !isActivated) {
            ticket.setInfo("Renewal is not possible for the provided license");
            ticket.setStatus("Error");
            return ticket;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(newLicense.getEndingDate());
        calendar.add(Calendar.DAY_OF_MONTH, Math.toIntExact(newLicense.getDuration()));
        newLicense.setEndingDate(calendar.getTime());

        licenseRepository.save(newLicense);
        licenseHistoryService.createNewRecord("Renewal", "License is valid", user, newLicense);

        ticket = createTicket(user, null, newLicense, "License successfully renewed", "OK");

        return ticket;
    }

}
