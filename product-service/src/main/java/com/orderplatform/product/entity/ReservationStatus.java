package com.orderplatform.product.entity;

public enum ReservationStatus {
    /** Stock is held for the order. */
    RESERVED,
    /** The order was cancelled and the stock went back to the product. */
    RELEASED
}
