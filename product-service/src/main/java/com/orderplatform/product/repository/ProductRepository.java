package com.orderplatform.product.repository;

import com.orderplatform.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Catalogue queries say "not deleted" explicitly (DeletedAtIsNull). Plain findById still sees
 * deleted products on purpose: cancelling an order must be able to give their stock back.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByDeletedAtIsNull(Pageable pageable);

    Optional<Product> findByIdAndDeletedAtIsNull(Long id);

    /** Includes deleted products: a deleted product's SKU is never reused. */
    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);
}
