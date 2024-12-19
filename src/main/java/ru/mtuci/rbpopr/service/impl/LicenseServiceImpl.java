package ru.mtuci.rbpopr.service.impl;

import org.springframework.stereotype.Service;
import ru.mtuci.rbpopr.model.*;
import ru.mtuci.rbpopr.repository.LicenseRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.security.*;
import java.util.stream.Collectors;


//TODO: 1. Некоторые проверки дублируются контроллером
//TODO: 2. licenseRepository.findByIdInAndCode кажется лишним, т.к. applicationDeviceLicensesList уже содержит информацию по связным лицензиям
//TODO: 3. createTicket на вид содержит лишние проверки
//TODO: 4. Почему status не входит в подпись тикета?
//TODO: 5. При активации лицензии дата первой активации никак не влияет на дату окончания
//TODO: 6.  updateLicense - owner не может быть null
//TODO: 7. Есть дублирующиеся проверки, целесообразно их вынести в отдельный метод


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
                              LicenseHistoryServiceImpl licenseHistoryService, UserDetailsServiceImpl userDetailsServiceImpl,
                              DeviceServiceImpl deviceServiceImpl) {
        this.licenseRepository = licenseRepository;
        this.licenseTypeService = licenseTypeService;
        this.productService = productService;
        this.deviceLicenseService = deviceLicenseService;
        this.licenseHistoryService = licenseHistoryService;
        this.userDetailsServiceImpl = userDetailsServiceImpl;
        this.deviceServiceImpl = deviceServiceImpl;
    }

    private ApplicationLicense getValidLicense(Long licenseId) {
        return licenseRepository.findById(licenseId)
                .orElseThrow(() -> new IllegalArgumentException("The specified license could not be found"));
    }

    private void validateProductAndType(Long productId, Long typeId) {
        productService.getProductById(productId)
                .orElseThrow(() -> new IllegalArgumentException("The specified product does not exist"));

        licenseTypeService.getLicenseTypeById(typeId)
                .orElseThrow(() -> new IllegalArgumentException("The specified license type could not be found"));
    }

    private void validateOwner(Long ownerId) {
        userDetailsServiceImpl.getUserById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Owner does not exist"));
    }

    private void validateLicenseForRenewal(ApplicationLicense license, ApplicationUser user) {
        boolean isBlocked = license.isBlocked();
        boolean hasExpired = license.getEndingDate() != null && new Date().after(license.getEndingDate());
        boolean isOwner = Objects.equals(license.getOwnerId().getId(), user.getId());
        boolean isActivated = license.getFirstActivationDate() != null;

        if (isBlocked || hasExpired || !isOwner || !isActivated) {
            throw new IllegalArgumentException("Renewal is not possible for the provided license");
        }
    }

    private void validateLicenseForActivation(ApplicationLicense license, ApplicationUser user, ApplicationDevice device) {
        if (license.isBlocked()) {
            throw new IllegalArgumentException("License is blocked");
        }
        if (deviceLicenseService.getDeviceCountForLicense(license.getId()) >= license.getDeviceCount()) {
            throw new IllegalArgumentException("Device limit exceeded for this license");
        }
        if (license.getUser() != null && !Objects.equals(license.getUser().getId(), user.getId())) {
            throw new IllegalArgumentException("This license is already activated by another user.");
        }
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

    public String updateLicense(Long id, Long ownerId, Long productId, Long typeId, Boolean isBlocked,
                                String description, Long deviceCount) {
        ApplicationLicense existingLicense = getValidLicense(id);
        existingLicense.setCode(String.valueOf(UUID.randomUUID()));

        validateProductAndType(productId, typeId);
        existingLicense.setProduct(productService.getProductById(productId).orElse(null));
        existingLicense.setLicenseType(licenseTypeService.getLicenseTypeById(typeId).orElse(null));
        existingLicense.setDuration(licenseTypeService.getLicenseTypeById(typeId).get().getDefaultDuration());

        validateOwner(ownerId);
        existingLicense.setOwnerId(userDetailsServiceImpl.getUserById(ownerId).orElse(null));

        existingLicense.setBlocked(isBlocked);
        existingLicense.setDescription(description);
        existingLicense.setDeviceCount(deviceCount);

        licenseRepository.save(existingLicense);
        return "License updated successfully";
    }

    public ApplicationTicket renewalLicense(String code, ApplicationUser user) {
        ApplicationTicket ticket = new ApplicationTicket();
        ApplicationLicense existingLicense = licenseRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("This license key is invalid"));

        try {
            validateLicenseForRenewal(existingLicense, user);
        } catch (IllegalArgumentException e) {
            ticket.setInfo(e.getMessage());
            ticket.setStatus("Error");
            return ticket;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(existingLicense.getEndingDate());
        calendar.add(Calendar.DAY_OF_MONTH, Math.toIntExact(existingLicense.getDuration()));
        existingLicense.setEndingDate(calendar.getTime());

        licenseRepository.save(existingLicense);
        licenseHistoryService.createNewRecord("Renewal", "License is valid", user, existingLicense);
        return createTicket(user, null, existingLicense, "License successfully renewed", "OK");
    }

    public ApplicationTicket activateLicense(String code, ApplicationDevice device, ApplicationUser user) {
        ApplicationTicket ticket = new ApplicationTicket();
        ApplicationLicense existingLicense = licenseRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("License not found"));

        try {
            validateLicenseForActivation(existingLicense, user, device);

        } catch (IllegalArgumentException e) {
            ticket.setInfo(e.getMessage());
            ticket.setStatus("Error");
            deviceServiceImpl.deleteLastDevice(user);
            return ticket;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DAY_OF_MONTH, Math.toIntExact(existingLicense.getDuration()));
        existingLicense.setEndingDate(calendar.getTime());

        if (existingLicense.getFirstActivationDate() == null) {
            existingLicense.setFirstActivationDate(new Date());
        }

        existingLicense.setUser(user);
        deviceLicenseService.createDeviceLicense(existingLicense, device);
        licenseRepository.save(existingLicense);
        licenseHistoryService.createNewRecord("Activated", "License is valid", user, existingLicense);

        return createTicket(user, device, existingLicense, "License successfully activated", "OK");
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


    private ApplicationTicket createTicket(ApplicationUser user, ApplicationDevice device,
                                           ApplicationLicense license, String info, String status) {
        ApplicationTicket ticket = new ApplicationTicket();
        ticket.setCurrentDate(new Date());
        ticket.setUserId(user != null ? user.getId() : null);
        ticket.setDeviceId(device != null ? device.getId() : null);

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
        ticket.setStatus(status);
        String signature = makeSignature(ticket);
        ticket.setDigitalSignature(signature);

        return ticket;
    }


    public ApplicationTicket getActiveLicensesForDevice(ApplicationDevice device, String code) {
        ApplicationTicket ticket = new ApplicationTicket();
        List<ApplicationDeviceLicense> applicationDeviceLicensesList = deviceLicenseService.getAllLicenseById(device);

        Optional<ApplicationDeviceLicense> matchingLicense = applicationDeviceLicensesList.stream()
                .filter(license -> license.getLicense() != null && code.equals(license.getLicense().getCode()))
                .findFirst();

        if (matchingLicense.isEmpty()) {
            ticket.setInfo("License was not found");
            ticket.setStatus("Error");
            return ticket;
        }

        ApplicationLicense license = matchingLicense.get().getLicense();

        return createTicket(license.getUser(), device, license, "Info about license", "OK");
    }
}
