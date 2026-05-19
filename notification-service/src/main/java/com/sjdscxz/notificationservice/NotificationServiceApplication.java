package com.sjdscxz.notificationservice;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@SpringBootApplication
@EnableKafka
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}

@Component
class OrderPlacedListener {
    private final List<String> deliveryLog = new CopyOnWriteArrayList<>();
    private final Counter deliveredCounter;

    OrderPlacedListener(MeterRegistry registry) {
        this.deliveredCounter = Counter.builder("notifications.delivered")
                .description("Number of order-placed notifications delivered")
                .register(registry);
    }

    @KafkaListener(topics = "order.placed", groupId = "notification-service")
    public void onOrderPlaced(String message) {
        String entry = "Sent confirmation for: " + message;
        deliveryLog.add(entry);
        deliveredCounter.increment();
        System.out.println("[notification-service] " + entry);
    }

    public List<String> recent() {
        int from = Math.max(0, deliveryLog.size() - 20);
        return new ArrayList<>(deliveryLog.subList(from, deliveryLog.size()));
    }
}

@RestController
class NotificationController {
    private final OrderPlacedListener listener;
    NotificationController(OrderPlacedListener listener) { this.listener = listener; }

    @GetMapping("/api/notifications/recent")
    public List<String> recent() {
        return listener.recent();
    }

    @GetMapping("/api/notifications/health")
    public java.util.Map<String, String> health() {
        return Collections.singletonMap("status", "OK");
    }
}
