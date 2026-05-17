package com.microservices.propertyaccessservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

@Configuration
@SpringBootApplication
public class PropertyAccessServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PropertyAccessServiceApplication.class, args);
    }

    @Bean
    public Docket productApi() {
        return new Docket(DocumentationType.SWAGGER_2).select()
                .apis(RequestHandlerSelectors.
                        basePackage("com.techefx.microservices.techefxpropertyaccessservice.controller"))
                .build();
    }


}
