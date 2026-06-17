package com.example.cashflow.shared.observability;



import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CorrelationFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnSameCorrelationIdWhenHeaderIsProvided() throws Exception {

        mockMvc.perform(get("/actuator/health")
            .header(CorrelationIdContext.HEADER_NAME, "test-correlation-001"))
            .andExpect(status().isOk())
            .andExpect(header().string(CorrelationIdContext.HEADER_NAME, "test-correlation-001")
        );
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderIsMissing() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(header().exists(CorrelationIdContext.HEADER_NAME));
    }
}
