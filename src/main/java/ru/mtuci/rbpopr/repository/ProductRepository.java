package ru.mtuci.rbpopr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mtuci.rbpopr.model.ApplicationProduct;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<ApplicationProduct, Long> {
    Optional<ApplicationProduct> findById(Long id);
    Optional<ApplicationProduct> findTopByOrderByIdDesc();
}
