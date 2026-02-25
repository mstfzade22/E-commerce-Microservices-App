package com.ecommerce.productservice.config;

import com.ecommerce.productservice.dto.event.InventoryStockUpdatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:product-service-group}")
    private String groupId;

    private final ObjectMapper kafkaObjectMapper;

    @Bean
    public Deserializer<InventoryStockUpdatedEvent> inventoryEventDeserializer() {
        return new Deserializer<>() {
            @Override
            public InventoryStockUpdatedEvent deserialize(String topic, byte[] data) {
                if (data == null) {
                    return null;
                }
                try {
                    return kafkaObjectMapper.readValue(data, InventoryStockUpdatedEvent.class);
                } catch (Exception e) {
                    log.error("Error deserializing InventoryStockUpdatedEvent: {}", e.getMessage());
                    throw new RuntimeException("Error deserializing message", e);
                }
            }
        };
    }

    @Bean
    public ConsumerFactory<String, InventoryStockUpdatedEvent> inventoryEventConsumerFactory(
            Deserializer<InventoryStockUpdatedEvent> inventoryEventDeserializer) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);

        log.info("Kafka consumer configured with bootstrap servers: {}, group: {}", bootstrapServers, groupId);

        return new DefaultKafkaConsumerFactory<>(configProps, new StringDeserializer(), inventoryEventDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InventoryStockUpdatedEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, InventoryStockUpdatedEvent> inventoryEventConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, InventoryStockUpdatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(inventoryEventConsumerFactory);

        // Configure error handler with retry (3 retries, 1 second interval)
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, exception) -> log.error("Failed to process message after retries - topic: {}, partition: {}, offset: {}, error: {}",
                        record.topic(), record.partition(), record.offset(), exception.getMessage()),
                new FixedBackOff(1000L, 3L)
        );
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

}
