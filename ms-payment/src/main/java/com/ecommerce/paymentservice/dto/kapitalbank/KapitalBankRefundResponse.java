package com.ecommerce.paymentservice.dto.kapitalbank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KapitalBankRefundResponse(
        @JsonProperty("tran") TranData tran,
        @JsonProperty("errorCode") String errorCode,
        @JsonProperty("errorDescription") String errorDescription
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TranData(
            @JsonProperty("approvalCode") String approvalCode,
            @JsonProperty("pmoResultCode") String pmoResultCode
    ) {}

    public boolean isSuccessful() {
        return tran != null && "1".equals(tran.pmoResultCode());
    }
}
