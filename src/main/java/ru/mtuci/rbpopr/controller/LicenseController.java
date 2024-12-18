package ru.mtuci.rbpopr.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.mtuci.rbpopr.configuration.JwtTokenProvider;
import ru.mtuci.rbpopr.model.*;
import ru.mtuci.rbpopr.service.impl.*;

import java.util.Objects;
import java.util.Optional;

//TODO: 1. productService.getProductById - можно оптимизировать, сделав только один запрос к БД

@RestController
@RequestMapping("/api/license")
@RequiredArgsConstructor
public class LicenseController {

    private final ProductServiceImpl productService;
    private final DeviceServiceImpl deviceService;
    private final UserDetailsServiceImpl userService;
    private final LicenseTypeServiceImpl licenseTypeService;
    private final LicenseServiceImpl licenseService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;

    @PostMapping("/create")
    @PreAuthorize("hasAnyAuthority('modification')")
    public ResponseEntity<String> createLicense(@RequestBody LicenseCreateRequest request, HttpServletRequest req) {
        try {
            Long productId = request.getProductId();
            Long ownerId = request.getOwnerId();
            Long licenseTypeId = request.getLicenseTypeId();

            String email = jwtTokenProvider.getUsername(req.getHeader("Authorization").substring(7));
            ApplicationUser user = userDetailsService.getUserByEmail(email).get();
            Optional<ApplicationProduct> product = productService.getProductById(productId);

            if (licenseTypeService.getLicenseTypeById(licenseTypeId).isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("License type with given ID does not exist.");
            }

            if (userService.getUserById(ownerId).isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Owner with given ID does not exist.");
            }

            if (product.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Product with given ID does not exist.");
            }

            if (product.get().isBlocked()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("This product is not available.");
            }
            Long id = licenseService.createLicense(productId, ownerId, licenseTypeId, user, request.getCount());

            return ResponseEntity.status(HttpStatus.OK).body("License created successfully.\nID: " + id);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while creating the license. Please try again.");
        }
    }
    @PostMapping("/update")
    @PreAuthorize("hasAnyAuthority('modification')")
    public ResponseEntity<String> updateLicense(@RequestBody LicenseUpdateRequest request) {
        try {
            String res = licenseService.updateLicense(
                    request.getId(),
                    request.getOwnerId(),
                    request.getProductId(),
                    request.getTypeId(),
                    request.getIsBlocked(),
                    request.getDescription(),
                    request.getDeviceCount()
            );

            if (Objects.equals(res, "OK")) {
                return ResponseEntity.status(HttpStatus.OK).body("License updated successfully.");
            }

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while updating the license. Please try again.");
        }
    }

    @PostMapping("/activate")
    public ResponseEntity<?> activateLicense(@RequestBody ActivationRequest request, HttpServletRequest req) {
        try {
            String email = jwtTokenProvider.getUsername(req.getHeader("Authorization").substring(7));
            ApplicationUser user = userDetailsService.getUserByEmail(email).get();
            ApplicationDevice device = deviceService.registerOrUpdateDevice(request.getMac_address(), request.getName(), user, request.getDeviceId());

            ApplicationTicket ticket = licenseService.activateLicense(request.getActivationCode(), device, user);

            if (ticket.getStatus().equals("OK")) {
                return ResponseEntity.status(HttpStatus.OK).body(ticket);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ticket.getInfo());
            }
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while activating the license. Please try again.");
        }
    }

    @PostMapping("/info")
    public ResponseEntity<?> infoLicense(@RequestBody LicenseInfoRequest request, HttpServletRequest req) {
        try {
            String email = jwtTokenProvider.getUsername(req.getHeader("Authorization").substring(7));
            ApplicationUser user = userDetailsService.getUserByEmail(email).get();
            Optional<ApplicationDevice> device = deviceService.getDeviceByInfo(user, request.getMac_address(),
                    request.getName());

            ApplicationTicket ticket = licenseService.getActiveLicensesForDevice(device.orElse(null), request.getActivationCode());

            if (device.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("The device was not found.");
            }

            if (!ticket.getStatus().equals("OK")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ticket.getInfo());
            }

            return ResponseEntity.status(HttpStatus.OK).body(ticket);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while retrieving license information. Please try again.");
        }
    }

    @PostMapping("/renewal")
    public ResponseEntity<?> renewalLicense(@RequestBody LicenseRenewalRequest request, HttpServletRequest req) {
        try {
            String email = jwtTokenProvider.getUsername(req.getHeader("Authorization").substring(7));
            ApplicationUser user = userDetailsService.getUserByEmail(email).get();

            ApplicationTicket ticket = licenseService.renewalLicense(request.getActivationCode(), user);

            return ResponseEntity.status(ticket.getStatus().equals("OK") ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                    .body(ticket.getStatus().equals("OK") ? ticket : ticket.getInfo());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Oops, something went wrong....");
        }
    }

}
