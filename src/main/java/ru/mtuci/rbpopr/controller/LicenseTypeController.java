package ru.mtuci.rbpopr.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mtuci.rbpopr.model.LicenseTypeCreationRequest;
import ru.mtuci.rbpopr.model.LicenseTypeUpdateRequest;
import ru.mtuci.rbpopr.service.impl.LicenseTypeServiceImpl;

import java.util.Objects;

@RestController
@RequestMapping("/api/type")
@RequiredArgsConstructor
public class LicenseTypeController {

    private final LicenseTypeServiceImpl licenseTypeService;

    @PostMapping("/create")
    @PreAuthorize("hasAnyAuthority('modification')")
    public ResponseEntity<String> createLicenseType(@RequestBody LicenseTypeCreationRequest request) {
        Long id;
        try {
            id = licenseTypeService.createLicenseType(request.getDuration(), request.getDescription(), request.getName());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Oops, something went wrong....");
        }

        return ResponseEntity.status(HttpStatus.OK).body("New type added successfully.\nID: " + id);
    }
    @PostMapping("/update")
    @PreAuthorize("hasAnyAuthority('modification')")
    public ResponseEntity<String> updateLicenseType(@RequestBody LicenseTypeUpdateRequest request) {
        try {
            String res = licenseTypeService.updateLicenseType(request.getId(), request.getDuration(),
                    request.getDescription(), request.getName());

            return ResponseEntity.status(Objects.equals(res, "OK") ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                    .body(Objects.equals(res, "OK") ? "New type added successfully." : res);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Oops, something went wrong....");
        }
    }
}
