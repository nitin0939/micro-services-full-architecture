package com.microservices.zuulapigatewayservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;

@SpringBootApplication
@EnableZuulProxy
@EnableEurekaClient
public class ZuulApiGatewayServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZuulApiGatewayServerApplication.class, args);
    }
// localhost:8766/ms-product-stock-service/check-product-stock/productName/bat/productAvailability/yes
// localhost:8766/ms-product-enquiry-service/product-enquiry/name/bat/availability/yes/unit/3
}
