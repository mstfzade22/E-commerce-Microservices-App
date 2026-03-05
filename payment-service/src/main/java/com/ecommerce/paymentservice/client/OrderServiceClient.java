package com.ecommerce.paymentservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@Slf4j
public class OrderServiceClient {

    private final RestClient restClient;

    public OrderServiceClient(@Value("${services.order-service.url}") String orderServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(orderServiceUrl)
                .build();
    }

    public String getOrderStatus(String orderNumber, String accessToken) {
        log.debug("Calling Order Service GET /orders/{}", orderNumber);
        Map<String, Object> response = restClient.get()
                .uri("/orders/{orderNumber}", orderNumber)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response != null && response.containsKey("status")) {
            return response.get("status").toString();
        }
        throw new RuntimeException("Unable to retrieve order status for: " + orderNumber);
    }
}
