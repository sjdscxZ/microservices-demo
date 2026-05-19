package com.sjdscxz.orderservice;

import jakarta.persistence.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;

@SpringBootApplication
@EnableKafka
public class OrderServiceApplication {
    public static void main(String[] args) { SpringApplication.run(OrderServiceApplication.class, args); }
}

@Entity
@Table(name = "orders")
class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable = false) Long userId;
    @Column(nullable = false, length = 200) String item;
    @Column(nullable = false, precision = 12, scale = 2) BigDecimal amount;
    @Column(nullable = false, length = 20) String status = "PLACED";
    @Column(nullable = false, updatable = false) Instant createdAt = Instant.now();

    protected Order() {}
    Order(Long userId, String item, BigDecimal amount) {
        this.userId = userId; this.item = item; this.amount = amount;
    }
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getItem() { return item; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}

interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
}

@RestController
@RequestMapping("/api/orders")
class OrderController {
    private final OrderRepository repo;
    private final KafkaTemplate<String, String> kafka;

    OrderController(OrderRepository repo, KafkaTemplate<String, String> kafka) {
        this.repo = repo; this.kafka = kafka;
    }

    @GetMapping
    public List<Order> list() { return repo.findAll(); }

    @GetMapping("/user/{userId}")
    public List<Order> byUser(@PathVariable Long userId) { return repo.findByUserId(userId); }

    @PostMapping
    public ResponseEntity<Order> create(@RequestBody CreateOrderRequest req) {
        Order saved = repo.save(new Order(req.userId(), req.item(), req.amount()));
        kafka.send("order.placed", String.valueOf(saved.id),
                "{\"orderId\":" + saved.id + ",\"userId\":" + saved.userId +
                ",\"amount\":" + saved.amount + "}");
        return ResponseEntity.created(URI.create("/api/orders/" + saved.id)).body(saved);
    }

    record CreateOrderRequest(Long userId, String item, BigDecimal amount) {}
}

@Component
class UserCreatedListener {
    @KafkaListener(topics = "user.created", groupId = "order-service")
    public void onUserCreated(String message) {
        System.out.println("[order-service] saw new user: " + message);
    }
}
