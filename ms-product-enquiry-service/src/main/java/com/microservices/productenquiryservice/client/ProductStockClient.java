package com.microservices.productenquiryservice.client;

import com.microservices.productenquiryservice.beans.ProductEnquiryBean;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

//@FeignClient(name="ms-product-stock-service", url="localhost:8800")
@FeignClient(name="ms-product-stock-service")
public interface ProductStockClient {

   @GetMapping("/check-product-stock/productName/{productName}/productAvailability/{productAvailability}")
    public ProductEnquiryBean checkProductStock(@PathVariable String productName,
                                              @PathVariable String productAvailability);
    // localhost:8700/product-enquiry/name/bat/availability/yes/unit/3
    // run stock on multiple servers and call above api here feignClient will be used as load balancer
    // other then feign we can use api Gateway
    // and mention all the services inside eureka/api gateway
    /* Spring Ribbon(Load Balancer)
       Spring Eureka(Discovery Service)
       Spring Zool(Api Gateway)
    */
}
