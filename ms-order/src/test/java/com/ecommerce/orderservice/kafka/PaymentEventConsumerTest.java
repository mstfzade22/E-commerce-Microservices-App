package com.ecommerce.orderservice.kafka;

import com.ecommerce.orderservice.entity.ProcessedEvent;
import com.ecommerce.orderservice.repository.ProcessedEventRepository;
import com.ecommerce.orderservice.service.OrderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventConsumerTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    @Mock
    private OrderService orderService;
    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private PaymentEventConsumer paymentEventConsumer;

    private JsonNode paymentSuccessNode() {
        return objectMapper.valueToTree(new java.util.LinkedHashMap<>() {{
            put("eventId", UUID.randomUUID().toString());
            put("eventType", "PAYMENT_SUCCESS");
            put("paymentId", 1);
            put("orderNumber", "ORD-001");
            put("userId", UUID.randomUUID().toString());
            put("amount", 100.00);
            put("kapitalOrderId", "KAP-001");
            put("approvedAt", Instant.now().toString());
            put("timestamp", Instant.now().toString());
        }});
    }

    private JsonNode paymentFailedNode() {
        return objectMapper.valueToTree(new java.util.LinkedHashMap<>() {{
            put("eventId", UUID.randomUUID().toString());
            put("eventType", "PAYMENT_FAILED");
            put("paymentId", 1);
            put("orderNumber", "ORD-001");
            put("userId", UUID.randomUUID().toString());
            put("reason", "Card declined");
            put("failedAt", Instant.now().toString());
            put("timestamp", Instant.now().toString());
        }});
    }

    @Test
    void consumePaymentSuccess_callsHandlePaymentSuccess() {
        when(processedEventRepository.saveAndFlush(any(ProcessedEvent.class))).thenReturn(null);

        paymentEventConsumer.consumePaymentEvents(paymentSuccessNode());

        verify(orderService).handlePaymentSuccess("ORD-001");
    }

    @Test
    void consumePaymentFailed_callsHandlePaymentFailure() {
        when(processedEventRepository.saveAndFlush(any(ProcessedEvent.class))).thenReturn(null);

        paymentEventConsumer.consumePaymentEvents(paymentFailedNode());

        verify(orderService).handlePaymentFailure("ORD-001", "Card declined");
    }

    @Test
    void consumeDuplicateEvent_skipped() {
        when(processedEventRepository.saveAndFlush(any(ProcessedEvent.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate"));

        paymentEventConsumer.consumePaymentEvents(paymentSuccessNode());

        verifyNoInteractions(orderService);
    }

    @Test
    void consumeUnknownEventType_ignored() {
        JsonNode node = objectMapper.valueToTree(new java.util.LinkedHashMap<>() {{
            put("eventId", UUID.randomUUID().toString());
            put("eventType", "PAYMENT_INITIATED");
            put("timestamp", Instant.now().toString());
        }});

        when(processedEventRepository.saveAndFlush(any(ProcessedEvent.class))).thenReturn(null);

        paymentEventConsumer.consumePaymentEvents(node);

        verifyNoInteractions(orderService);
    }
}
