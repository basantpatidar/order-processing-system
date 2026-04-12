package com.basant.orderservice;

import com.basant.orderservice.dto.OrderDtos.*;
import com.basant.orderservice.kafka.OrderCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("OrderController Integration Tests")
class OrderControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("orders_db")
            .withUsername("orders_user")
            .withPassword("orders_pass");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/orders - creates order and returns 201")
    void createOrder_returns201() throws Exception {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId("prod-001");
        item.setQuantity(2);
        item.setPrice(new BigDecimal("49.99"));

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId("cust-integration-test");
        request.setItems(List.of(item));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value("cust-integration-test"))
                .andExpect(jsonPath("$.status").value("PAYMENT_PROCESSING"))
                .andExpect(jsonPath("$.totalAmount").value(99.98))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/orders - returns 400 for invalid payload")
    void createOrder_returns400_forEmptyItems() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId("cust-123");
        request.setItems(List.of()); // empty items - should fail validation

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/orders/{id} - returns 404 for unknown order")
    void getOrder_returns404_forUnknownId() throws Exception {
        mockMvc.perform(get("/api/orders/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/orders - publishes event to Kafka")
    void createOrder_publishesKafkaEvent() throws Exception {
        // Arrange
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId("prod-kafka-test");
        item.setQuantity(1);
        item.setPrice(new BigDecimal("25.00"));

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId("cust-kafka-test");
        request.setItems(List.of(item));

        // Act — place the order
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Assert — consume the Kafka event
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, OrderCreatedEvent.class.getName());

        try (KafkaConsumer<String, OrderCreatedEvent> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of("order.created"));
            Iterable<ConsumerRecord<String, OrderCreatedEvent>> records =
                    consumer.poll(Duration.ofSeconds(10));

            boolean found = false;
            for (ConsumerRecord<String, OrderCreatedEvent> record : records) {
                if ("cust-kafka-test".equals(record.value().getCustomerId())) {
                    found = true;
                    assertThat(record.value().getTotalAmount()).isEqualByComparingTo("25.00");
                }
            }
            assertThat(found).isTrue();
        }
    }
}
