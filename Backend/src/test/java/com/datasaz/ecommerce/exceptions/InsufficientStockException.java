package com.datasaz.ecommerce.exceptions;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
public class InsufficientStockException extends RuntimeException {

    private final String message;

//    public InsufficientStockException(Long productId, int requestedQuantity, int availableQuantity) {
//        super(String.format("Insufficient stock for product ID: %d, requested: %d, available: %d",
//                productId, requestedQuantity, availableQuantity));
//        this.message = String.format("Insufficient stock for product ID: %d, requested: %d, available: %d",
//                productId, requestedQuantity, availableQuantity);
//    }


}
