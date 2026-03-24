package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.dto.response.CartResponse;
import com.ecommerce.orderservice.dto.response.CartValidationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.UUID;

@Service
@Slf4j
public class CartServiceClient {

    private final RestClient restClient;

    public CartServiceClient(@Value("${services.cart-service.url}") String cartServiceUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
                .baseUrl(cartServiceUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public CartResponse getCart(UUID userId, String role) {
        log.debug("Calling Cart Service GET /cart");
        return restClient.get()
                .uri("/cart")
                .header("X-User-Id", userId.toString())
                .header("X-User-Role", role)
                .retrieve()
                .body(CartResponse.class);
    }

    public CartValidationResponse validateCart(UUID userId, String role) {
        log.debug("Calling Cart Service POST /cart/validate");
        return restClient.post()
                .uri("/cart/validate")
                .header("X-User-Id", userId.toString())
                .header("X-User-Role", role)
                .retrieve()
                .body(CartValidationResponse.class);
    }

    public void clearCart(UUID userId, String role) {
        log.debug("Calling Cart Service DELETE /cart");
        restClient.delete()
                .uri("/cart")
                .header("X-User-Id", userId.toString())
                .header("X-User-Role", role)
                .retrieve()
                .toBodilessEntity();
    }
}