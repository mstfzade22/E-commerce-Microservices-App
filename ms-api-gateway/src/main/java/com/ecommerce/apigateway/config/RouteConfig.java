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

                .route("ms-user-auth", r -> r
                        .path("/api/v1/auth/**")
                        .uri("http://ms-user:8081"))

                .route("ms-user", r -> r
                        .path("/api/v1/users/**")
                        .uri("http://ms-user:8081"))

                .route("ms-product", r -> r
                        .path("/api/v1/products/**")
                        .uri("http://ms-product:8082"))

                .route("ms-product-categories", r -> r
                        .path("/api/v1/categories/**")
                        .uri("http://ms-product:8082"))

                .route("ms-inventory", r -> r
                        .path("/api/v1/inventory/**")
                        .uri("http://ms-inventory:8083"))

                .route("ms-cart", r -> r
                        .path("/api/v1/cart/**")
                        .uri("http://ms-cart:8084"))

                .route("ms-order", r -> r
                        .path("/api/v1/orders/**")
                        .uri("http://ms-order:8085"))

                .route("ms-payment", r -> r
                        .path("/api/v1/payments/**")
                        .uri("http://ms-payment:8086"))

                .route("ms-notification", r -> r
                        .path("/api/v1/notifications/**")
                        .uri("http://ms-notification:8087"))

                .build();
    }
}
