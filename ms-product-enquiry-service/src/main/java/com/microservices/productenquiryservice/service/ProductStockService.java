package com.microservices.productenquiryservice.service;

import com.microservices.productenquiryservice.beans.ProductEnquiryBean;
import com.microservices.productenquiryservice.client.ProductStockClient;
import com.microservices.productenquiryservice.exception.ProductStockServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProductStockService {

    @Autowired
    private ProductStockClient productStockClient;

    @CircuitBreaker(name = "productStockService", fallbackMethod = "checkProductStockFallback")
    public ProductEnquiryBean checkProductStock(String productName, String productAvailability) {
        return productStockClient.checkProductStock(productName, productAvailability);
    }

    private ProductEnquiryBean checkProductStockFallback(String productName, String productAvailability, Throwable throwable) {
        throw new ProductStockServiceUnavailableException(
                "ms-product-stock-service is currently unavailable, please try again later");
    }
}
