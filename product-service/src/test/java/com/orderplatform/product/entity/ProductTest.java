package com.orderplatform.product.entity;

import com.orderplatform.product.exception.InsufficientStockException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    @Test
    void reserveAndReleaseMoveStock() {
        Product product = keyboard(3);

        product.reserve(3);
        assertThat(product.getStock()).isZero();

        product.release(2);
        assertThat(product.getStock()).isEqualTo(2);
    }

    @Test
    void reservingMoreThanTheStockIsRefusedAndChangesNothing() {
        Product product = keyboard(2);

        assertThatThrownBy(() -> product.reserve(3))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("requested 3, available 2");
        assertThat(product.getStock()).isEqualTo(2);
    }

    @Test
    void aDeletedProductCannotBeReservedButCanTakeStockBack() {
        Product product = keyboard(5);
        product.delete();

        assertThat(product.isDeleted()).isTrue();
        assertThatThrownBy(() -> product.reserve(1)).isInstanceOf(InsufficientStockException.class);

        product.release(2);
        assertThat(product.getStock()).isEqualTo(7);
    }

    @Test
    void deletingTwiceKeepsTheFirstDeletionTime() {
        Product product = keyboard(1);
        product.delete();
        var firstDeletion = product.getDeletedAt();

        product.delete();

        assertThat(product.getDeletedAt()).isEqualTo(firstDeletion);
    }

    @Test
    void quantitiesMustBePositive() {
        Product product = keyboard(2);

        assertThatThrownBy(() -> product.reserve(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> product.release(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void stockCannotBeSetNegative() {
        assertThatThrownBy(() -> keyboard(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nameIsTrimmedAndSkuIsTrimmedAndUpperCased() {
        Product product = new Product("  Keyboard ", " kb-100 ", new BigDecimal("1000.00"), 1);

        assertThat(product.getName()).isEqualTo("Keyboard");
        assertThat(product.getSku()).isEqualTo("KB-100");
    }

    private static Product keyboard(int stock) {
        return new Product("Keyboard", "kb-100", new BigDecimal("1000.00"), stock);
    }
}
