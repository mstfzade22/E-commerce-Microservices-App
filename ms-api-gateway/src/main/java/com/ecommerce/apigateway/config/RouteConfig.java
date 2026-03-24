package com.ecommerce.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()

                // Auth routes
                .route("ms-user-auth", r -> r
                        .path("/api/v1/auth/**")
                        .uri("http://ms-user:8081"))

                // User routes
                .route("ms-user", r -> r
                        .path("/api/v1/users/**")
                        .uri("http://ms-user:8081"))

                // Product routes
                .route("ms-product", r -> r
                        .path("/api/v1/products/**")
                        .uri("http://ms-product:8082"))

                // Category routes
                .route("ms-product-categories", r -> r
                        .path("/api/v1/categories/**")
                        .uri("http://ms-product:8082"))

                // Inventory routes
                .route("ms-inventory", r -> r
                        .path("/api/v1/inventory/**")
                        .uri("http://ms-inventory:8083"))

                // Cart routes
                .route("ms-cart", r -> r
                        .path("/api/v1/cart/**")
                        .uri("http://ms-cart:8084"))

                // Order routes
                .route("ms-order", r -> r
                        .path("/api/v1/orders/**")
                        .uri("http://ms-order:8085"))

                // Payment routes
                .route("ms-payment", r -> r
                        .path("/api/v1/payments/**")
                        .uri("http://ms-payment:8086"))

                // Notification routes
                .route("ms-notification", r -> r
                        .path("/api/v1/notifications/**")
                        .uri("http://ms-notification:8087"))

                .build();
    }
}
