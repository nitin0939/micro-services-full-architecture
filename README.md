# ms-property-access-service

## Architecture
END Points
localhost:8100/access/accessPropertyFile
localhost:8100/actuator/refresh
// run stock-service and product-enquiry-service on multiple ports
localhost:8800/check-product-stock/productName/bat/productAvailability/yes
localhost:8700/product-enquiry/name/bat/availability/yes/unit/3
localhost:8766/ms-product-enquiry-service/product-enquiry/name/bat/availability/yes/unit/3
localhost:8766/ms-product-stock-service/check-product-stock/productName/bat/productAvailability/yes
localhost:8900/product-enquiry/name/bat/availability/yes/unit/3

![Architecture](ms-property-access-service/architecture.png)
