package com.ecommerce.inventoryservice.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@Slf4j
public class KafkaConsumerConfig {

    private final String bootstrapServers;
    private final ObjectMapper kafkaObjectMapper;

    public KafkaConsumerConfig(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Qualifier("kafkaObjectMapper") ObjectMapper kafkaObjectMapper) {
        this.bootstrapServers = bootstrapServers;
        this.kafkaObjectMapper = kafkaObjectMapper;
    }

    @Bean
    public Deserializer<JsonNode> jsonNodeDeserializer() {
        return new Deserializer<>() {
            @Override
            public JsonNode deserialize(String topic, byte[] data) {
                if (data == null) return null;
                try {
                    return kafkaObjectMapper.readTree(data);
                } catch (Exception e) {
                    log.error("Error deserializing message to JsonNode: {}", e.getMessage());
                    throw new RuntimeException("Error deserializing message", e);
                }
            }
        };
    }

    @Bean
    public ConsumerFactory<String, JsonNode> consumerFactory(Deserializer<JsonNode> jsonNodeDeserializer) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, KafkaTopicConfig.INVENTORY_SERVICE_GROUP);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        log.info("Kafka consumer configured with bootstrap servers: {}, group: {}", bootstrapServers, KafkaTopicConfig.INVENTORY_SERVICE_GROUP);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), jsonNodeDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, JsonNode> kafkaListenerContainerFactory(
            ConsumerFactory<String, JsonNode> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, JsonNode> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, exception) -> log.error("Failed to process message after retries - topic: {}, partition: {}, offset: {}, error: {}",
                        record.topic(), record.partition(), record.offset(), exception.getMessage()),
                new FixedBackOff(1000L, 3L)
        );
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}
