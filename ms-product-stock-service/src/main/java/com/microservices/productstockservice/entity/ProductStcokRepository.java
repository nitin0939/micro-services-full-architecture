package com.microservices.productstockservice.entity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductStcokRepository extends JpaRepository<ProductStock, Long> {

    ProductStock findByProductNameAndProductAvailability(String productName, String productAvailability);

}
