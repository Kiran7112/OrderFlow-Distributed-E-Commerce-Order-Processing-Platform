package com.orderflow.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("order-service", r -> r
                        .path("/api/orders/**")
                        .filters(f -> f.filter(new JwtAuthFilter()))
                        .uri("http://order-service:8081"))

                .route("inventory-service", r -> r
                        .path("/api/inventory/**")
                        .filters(f -> f.filter(new JwtAuthFilter()))
                        .uri("http://inventory-service:8082"))

                .route("payment-service", r -> r
                        .path("/api/payments/**")
                        .filters(f -> f.filter(new JwtAuthFilter()))
                        .uri("http://payment-service:8083"))

                .route("shipping-service", r -> r
                        .path("/api/shipping/**")
                        .filters(f -> f.filter(new JwtAuthFilter()))
                        .uri("http://shipping-service:8085"))

                .route("analytics-service", r -> r
                        .path("/api/analytics/**")
                        .filters(f -> f.filter(new JwtAuthFilter()))
                        .uri("http://analytics-service:8086"))

                .route("auth", r -> r
                        .path("/auth/**")
                        .uri("http://order-service:8081"))

                .build();
    }
}
