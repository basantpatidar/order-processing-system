package com.basant.orderservice.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${kafka.topics.order-created}")
    private String orderCreatedTopic;

    @Value("${kafka.topics.order-created-dlt}")
    private String orderCreatedDlt;

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name(orderCreatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderCreatedDlt() {
        // Dead Letter Topic — failed messages land here after all retries exhausted
        return TopicBuilder.name(orderCreatedDlt)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
