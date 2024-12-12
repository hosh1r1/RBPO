package ru.mtuci.rbpopr.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mtuci.rbpopr.model.*;
import ru.mtuci.rbpopr.service.impl.ProductServiceImpl;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductServiceImpl productService;

    @PostMapping("/create")
    @PreAuthorize("hasAnyAuthority('modification')")
    public ResponseEntity<String> createProduct(@RequestBody ProductCreateRequest request) {
        try {
            Long id = productService.createProduct(request.getName(), request.getIsBlocked());
            return ResponseEntity.ok("Product has been successfully created. ID: " + id);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while creating the product. Please try again.");
        }
    }
    @PostMapping("/update")
    @PreAuthorize("hasAnyAuthority('modification')")
    public ResponseEntity<String> updateProduct(@RequestBody ProductUpdateRequest request) {
        try {
            String res = productService.updateProduct(request.getProductId(), request.getName(), request.getIsBlocked());

            if (res.equals("Product not found") || res.equals("Invalid product data")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(res);
            }

            return ResponseEntity.status(HttpStatus.OK).body("Product updated successfully.");
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while updating the product. Please try again.");
        }
    }
}
