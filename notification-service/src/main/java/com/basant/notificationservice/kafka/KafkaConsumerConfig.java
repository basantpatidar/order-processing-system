package com.basant.notificationservice.kafka;

import com.basant.notificationservice.dto.EventDtos.OrderCreatedEventDto;
import com.basant.notificationservice.dto.EventDtos.PaymentProcessedEventDto;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private Map<String, Object> baseConsumerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        return props;
    }

    // ── Factory for order.created ──────────────────────────────────────────────

    @Bean
    public ConsumerFactory<String, OrderCreatedEventDto> orderCreatedConsumerFactory() {
        JsonDeserializer<OrderCreatedEventDto> deserializer =
                new JsonDeserializer<>(OrderCreatedEventDto.class, false);
        deserializer.addTrustedPackages("*");
        return new DefaultKafkaConsumerFactory<>(baseConsumerProps(),
                new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEventDto>
    orderCreatedContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEventDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderCreatedConsumerFactory());
        return factory;
    }

    // ── Factory for payment.processed ─────────────────────────────────────────

    @Bean
    public ConsumerFactory<String, PaymentProcessedEventDto> paymentProcessedConsumerFactory() {
        JsonDeserializer<PaymentProcessedEventDto> deserializer =
                new JsonDeserializer<>(PaymentProcessedEventDto.class, false);
        deserializer.addTrustedPackages("*");
        return new DefaultKafkaConsumerFactory<>(baseConsumerProps(),
                new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentProcessedEventDto>
    paymentProcessedContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, PaymentProcessedEventDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(paymentProcessedConsumerFactory());
        return factory;
    }
}
