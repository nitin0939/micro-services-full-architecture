package com.microservices.productenquiryservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients("com.microservices.productenquiryservice")
@EnableEurekaClient
public class ProductEnquiryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductEnquiryServiceApplication.class, args);
    }

}
