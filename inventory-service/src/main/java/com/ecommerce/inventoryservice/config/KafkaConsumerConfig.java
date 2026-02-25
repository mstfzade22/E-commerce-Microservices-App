package com.ecommerce.inventoryservice.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:29092}")
    private String bootstrapServers;

    private final ObjectMapper kafkaObjectMapper;

    public KafkaConsumerConfig(@Qualifier("kafkaObjectMapper") ObjectMapper kafkaObjectMapper) {
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
                    throw new RuntimeException("Error deserializing to JsonNode", e);
                }
            }
        };
    }

    @Bean
    public ConsumerFactory<String, JsonNode> consumerFactory(Deserializer<JsonNode> jsonNodeDeserializer) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "inventory-service-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), jsonNodeDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, JsonNode> kafkaListenerContainerFactory(
            ConsumerFactory<String, JsonNode> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, JsonNode> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
}