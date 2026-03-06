package com.ecommerce.paymentservice.dto.kapitalbank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KapitalBankOrderResponse(
        @JsonProperty("order") OrderData order
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrderData(
            @JsonProperty("id") Long id,
            @JsonProperty("hppUrl") String hppUrl,
            @JsonProperty("password") String password,
            @JsonProperty("status") String status,
            @JsonProperty("cvv2AuthStatus") String cvv2AuthStatus,
            @JsonProperty("secret") String secret
    ) {}
}
