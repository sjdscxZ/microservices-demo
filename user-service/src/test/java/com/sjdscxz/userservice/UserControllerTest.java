package com.sjdscxz.userservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {"user.created"})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.datasource.url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
})
class UserControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void listReturnsEmptyArrayInitially() throws Exception {
        mvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void createReturns201WithLocationHeader() throws Exception {
        String body = mapper.writeValueAsString(
                new UserController.CreateUserRequest("Alice", "alice@example.com"));
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void duplicateEmailReturns409() throws Exception {
        String body = mapper.writeValueAsString(
                new UserController.CreateUserRequest("Bob", "bob@example.com"));
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void getMissingReturns404() throws Exception {
        mvc.perform(get("/api/users/9999")).andExpect(status().isNotFound());
    }

    @Test
    void actuatorHealthIsExposed() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
