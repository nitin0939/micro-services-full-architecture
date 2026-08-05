package com.microservices.productenquiryservice.exception;

public class ProductStockServiceUnavailableException extends RuntimeException {

    public ProductStockServiceUnavailableException(String message) {
        super(message);
    }
}
