package com.sjdscxz.userservice;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository repo;
    private final KafkaTemplate<String, String> kafka;

    public UserController(UserRepository repo, KafkaTemplate<String, String> kafka) {
        this.repo = repo;
        this.kafka = kafka;
    }

    @GetMapping
    public List<User> list() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> get(@PathVariable Long id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<User> create(@Valid @RequestBody CreateUserRequest req) {
        if (repo.existsByEmail(req.email())) {
            return ResponseEntity.status(409).build();
        }
        User saved = repo.save(new User(req.name(), req.email()));
        kafka.send("user.created", String.valueOf(saved.getId()),
                "{\"id\":" + saved.getId() + ",\"email\":\"" + saved.getEmail() + "\"}");
        return ResponseEntity.created(URI.create("/api/users/" + saved.getId())).body(saved);
    }

    public record CreateUserRequest(String name, String email) {}
}
