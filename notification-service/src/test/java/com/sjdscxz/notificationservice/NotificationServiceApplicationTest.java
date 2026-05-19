package com.sjdscxz.notificationservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"order.placed"})
@TestPropertySource(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
class NotificationServiceApplicationTest {

    @Test
    void contextLoads() {
        // Smoke test: Spring context boots, Kafka listener registers.
    }
}
