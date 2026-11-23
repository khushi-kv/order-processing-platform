package com.orderplatform.product.entity;

import com.orderplatform.product.exception.InsufficientStockException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;

/** A product in the catalogue. Stock only changes through {@link #reserve} and {@link #release}. */
@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 64)
    private String sku;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stock;

    /**
     * Optimistic locking: Hibernate adds "WHERE version = ?" to every UPDATE and increments it.
     * If another transaction changed the row first, nothing matches and the update fails instead
     * of overwriting it. This is what stops two orders selling the same last unit.
     */
    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Soft delete: null while the product is on sale. */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    public Product(String name, String sku, BigDecimal price, int stock) {
        update(name, sku, price, stock);
    }

    /** Replaces the editable fields; setting stock is how an admin restocks. */
    public void update(String name, String sku, BigDecimal price, int stock) {
        if (stock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }
        this.name = name.trim();
        this.sku = normalizeSku(sku);
        this.price = price;
        this.stock = stock;
    }

    /** Takes stock for an order; refuses rather than going below zero or selling a deleted product. */
    public void reserve(int quantity) {
        requirePositive(quantity);
        if (isDeleted()) {
            throw new InsufficientStockException("Product " + id + " is not available");
        }
        if (quantity > stock) {
            throw new InsufficientStockException(
                    "Insufficient stock for product " + id + " (requested " + quantity + ", available " + stock + ")");
        }
        stock -= quantity;
    }

    /** Gives stock back when an order that reserved it is cancelled; works even if the product was deleted since. */
    public void release(int quantity) {
        requirePositive(quantity);
        stock += quantity;
    }

    /** Soft delete: hides the product and blocks new reservations, but keeps the row. */
    public void delete() {
        if (deletedAt == null) {
            deletedAt = Instant.now();
        }
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /** SKUs are stored trimmed and upper-cased so "kb-100" and "KB-100" are the same product. */
    public static String normalizeSku(String sku) {
        return sku.trim().toUpperCase(Locale.ROOT);
    }

    private static void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
