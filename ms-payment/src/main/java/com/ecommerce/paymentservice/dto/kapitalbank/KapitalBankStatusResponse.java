package com.ecommerce.paymentservice.dto.kapitalbank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KapitalBankStatusResponse(
        @JsonProperty("order") OrderData order
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrderData(
            @JsonProperty("id") Long id,
            @JsonProperty("status") String status,
            @JsonProperty("prevStatus") String prevStatus,
            @JsonProperty("amount") Double amount,
            @JsonProperty("currency") String currency
    ) {}
}
