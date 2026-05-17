package com.microservices.productstockservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
public class ProductStockServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductStockServiceApplication.class, args);
    }

}
