package com.ecommerce.paymentservice.client;

import com.ecommerce.paymentservice.dto.kapitalbank.KapitalBankOrderResponse;
import com.ecommerce.paymentservice.dto.kapitalbank.KapitalBankRefundResponse;
import com.ecommerce.paymentservice.dto.kapitalbank.KapitalBankStatusResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
@Slf4j
public class KapitalBankClient {

    @Value("${kapitalbank.api-url}")
    private String apiUrl;

    @Value("${kapitalbank.username}")
    private String username;

    @Value("${kapitalbank.password}")
    private String password;

    @Value("${kapitalbank.currency}")
    private String currency;

    @Value("${kapitalbank.language}")
    private String language;

    private HttpClient httpClient;
    private ObjectMapper objectMapper;
    private String basicAuthHeader;

    @PostConstruct
    public void init() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.basicAuthHeader = "Basic " + Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        log.info("KapitalBankClient initialized with Basic Auth for {}", apiUrl);
    }

    public KapitalBankOrderResponse createOrder(BigDecimal amount, String description, String redirectUrl) {
        Map<String, Object> body = Map.of(
                "order", Map.of(
                        "typeRid", "Order_SMS",
                        "amount", amount.toPlainString(),
                        "currency", currency,
                        "language", language,
                        "description", description,
                        "hppRedirectUrl", redirectUrl
                )
        );

        String responseBody = sendPostRequest(apiUrl + "/order", body);
        return parseResponse(responseBody, KapitalBankOrderResponse.class);
    }

    public KapitalBankStatusResponse getOrderStatus(String orderId) {
        String responseBody = sendGetRequest(apiUrl + "/order/" + orderId);
        return parseResponse(responseBody, KapitalBankStatusResponse.class);
    }

    public KapitalBankRefundResponse refundOrder(String orderId, BigDecimal amount) {
        Map<String, Object> body = Map.of(
                "tran", Map.of(
                        "phase", "Single",
                        "type", "Refund",
                        "amount", amount.toPlainString()
                )
        );

        String responseBody = sendPostRequest(apiUrl + "/order/" + orderId + "/exec-tran", body);
        return parseResponse(responseBody, KapitalBankRefundResponse.class);
    }

    public KapitalBankRefundResponse reverseOrder(String orderId) {
        Map<String, Object> body = Map.of(
                "tran", Map.of(
                        "phase", "Single",
                        "voidKind", "Full"
                )
        );

        String responseBody = sendPostRequest(apiUrl + "/order/" + orderId + "/exec-tran", body);
        return parseResponse(responseBody, KapitalBankRefundResponse.class);
    }

    private String sendPostRequest(String url, Object body) {
        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            log.debug("Sending POST to {}: {}", url, jsonBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", basicAuthHeader)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("Kapital Bank response status: {}, body: {}", response.statusCode(), response.body());

            if (response.statusCode() >= 400) {
                throw new RuntimeException("Kapital Bank returned HTTP " + response.statusCode() + ": " + response.body());
            }

            return response.body();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to communicate with Kapital Bank: {}", e.getMessage(), e);
            throw new RuntimeException("Kapital Bank communication error: " + e.getMessage(), e);
        }
    }

    private String sendGetRequest(String url) {
        try {
            log.debug("Sending GET to {}", url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", basicAuthHeader)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("Kapital Bank response status: {}, body: {}", response.statusCode(), response.body());

            if (response.statusCode() >= 400) {
                throw new RuntimeException("Kapital Bank returned HTTP " + response.statusCode() + ": " + response.body());
            }

            return response.body();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to communicate with Kapital Bank: {}", e.getMessage(), e);
            throw new RuntimeException("Kapital Bank communication error: " + e.getMessage(), e);
        }
    }

    private <T> T parseResponse(String responseBody, Class<T> type) {
        try {
            return objectMapper.readValue(responseBody, type);
        } catch (Exception e) {
            log.error("Failed to parse Kapital Bank response: {}", responseBody, e);
            throw new RuntimeException("Failed to parse Kapital Bank response: " + e.getMessage(), e);
        }
    }
}
