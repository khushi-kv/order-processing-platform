package com.orderplatform.product.exception;

/** A product cannot cover a reservation: not enough stock, or the product was deleted. */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String message) {
        super(message);
    }
}
