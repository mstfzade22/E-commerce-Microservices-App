package com.ecommerce.paymentservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class OrderServiceClient {

    private final RestClient restClient;

    public OrderServiceClient(@Value("${services.order-service.url}") String orderServiceUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
                .baseUrl(orderServiceUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public String getOrderStatus(String orderNumber, UUID userId, String role) {
        Map<String, Object> response = getOrderResponse(orderNumber, userId, role);
        if (response != null && response.containsKey("status")) {
            return response.get("status").toString();
        }
        throw new RuntimeException("Unable to retrieve order status for: " + orderNumber);
    }

    public OrderDetails getOrderDetails(String orderNumber, UUID userId, String role) {
        log.debug("Calling Order Service GET /orders/{}", orderNumber);
        Map<String, Object> response = getOrderResponse(orderNumber, userId, role);

        if (response == null) {
            throw new RuntimeException("Unable to retrieve order details for: " + orderNumber);
        }

        String status = response.containsKey("status") ? response.get("status").toString() : null;
        BigDecimal finalAmount = response.containsKey("finalAmount")
                ? new BigDecimal(response.get("finalAmount").toString())
                : null;

        if (status == null || finalAmount == null) {
            throw new RuntimeException("Order response missing required fields (status/finalAmount) for: " + orderNumber);
        }

        return new OrderDetails(status, finalAmount);
    }

    private Map<String, Object> getOrderResponse(String orderNumber, UUID userId, String role) {
        log.debug("Calling Order Service GET /orders/{}", orderNumber);
        return restClient.get()
                .uri("/orders/{orderNumber}", orderNumber)
                .header("X-User-Id", userId.toString())
                .header("X-User-Role", role)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public record OrderDetails(String status, BigDecimal finalAmount) {}
}
