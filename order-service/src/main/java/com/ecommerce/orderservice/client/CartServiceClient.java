package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.dto.response.CartResponse;
import com.ecommerce.orderservice.dto.response.CartValidationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class CartServiceClient {

    private final RestClient restClient;

    public CartServiceClient(@Value("${services.cart-service.url}") String cartServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(cartServiceUrl)
                .build();
    }

    public CartResponse getCart(String accessToken) {
        log.debug("Calling Cart Service GET /cart");
        return restClient.get()
                .uri("/cart")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(CartResponse.class);
    }

    public CartValidationResponse validateCart(String accessToken) {
        log.debug("Calling Cart Service POST /cart/validate");
        return restClient.post()
                .uri("/cart/validate")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(CartValidationResponse.class);
    }

    public void clearCart(String accessToken) {
        log.debug("Calling Cart Service DELETE /cart");
        restClient.delete()
                .uri("/cart")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .toBodilessEntity();
    }
}