package com.sjdscxz.orderservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {"order.placed", "user.created"})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.datasource.url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
})
class OrderControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void listReturns200() throws Exception {
        mvc.perform(get("/api/orders")).andExpect(status().isOk());
    }

    @Test
    void createOrderReturns201() throws Exception {
        String body = """
                {"userId": 1, "item": "Spring in Action", "amount": 39.99}
                """;
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.item").value("Spring in Action"));
    }

    @Test
    void actuatorPrometheusExposed() throws Exception {
        mvc.perform(get("/actuator/prometheus")).andExpect(status().isOk());
    }
}
