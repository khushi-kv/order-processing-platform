package com.orderplatform.product.repository;

import com.orderplatform.product.entity.ReservationStatus;
import com.orderplatform.product.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    /** Every line of an order, whatever its status. */
    List<StockReservation> findByOrderId(Long orderId);

    /** The lines an order still holds; releasing uses this so releasing twice does nothing. */
    List<StockReservation> findByOrderIdAndStatus(Long orderId, ReservationStatus status);
}
