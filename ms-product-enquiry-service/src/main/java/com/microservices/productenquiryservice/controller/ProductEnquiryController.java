package com.microservices.productenquiryservice.controller;

import com.microservices.productenquiryservice.beans.ProductEnquiryBean;
import com.microservices.productenquiryservice.exception.ProductStockServiceUnavailableException;
import com.microservices.productenquiryservice.service.ProductStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductEnquiryController {

    @Autowired
    ProductStockService productStockService;


    @GetMapping("/product-enquiry/name/{name}/availability/{availability}/unit/{unit}")
    public ProductEnquiryBean getEnquiryOfProduct(@PathVariable String name,
                                                  @PathVariable String availability,
                                                  @PathVariable int unit){



        ProductEnquiryBean productEnquiryBean=productStockService.checkProductStock(name,availability);

        double totalPrice=productEnquiryBean.getProductPrice().doubleValue()*unit;
        double discounts=productEnquiryBean.getDiscountOffer();
        double discountPrice=totalPrice-totalPrice*discounts/100;


        return new ProductEnquiryBean(
                productEnquiryBean.getId(),
                name,
                productEnquiryBean.getProductPrice(),
                availability,
                productEnquiryBean.getDiscountOffer(),
                unit,
                discountPrice,
                productEnquiryBean.getPort()

        );

    }

    @ExceptionHandler(ProductStockServiceUnavailableException.class)
    public ResponseEntity<String> handleProductStockServiceUnavailable(ProductStockServiceUnavailableException ex){
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ex.getMessage());
    }

}
