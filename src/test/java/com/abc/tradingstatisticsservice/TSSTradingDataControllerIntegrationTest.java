package com.abc.tradingstatisticsservice;

import com.abc.tradingstatisticsservice.dto.TradingDataBatch;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TSSTradingDataControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    void testAddBatchAndGetStats() throws Exception {
        // First, add a batch of data
        TradingDataBatch batch = new TradingDataBatch();
        batch.setSymbol("AAPL");
        batch.setValues(new Float[]{1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f});

        mockMvc.perform(post("/add_batch/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batch)))
                .andExpect(status().isOk())
                .andExpect(content().string("Batch added successfully."));

        // Now, fetch the stats for k=1 (which corresponds to 10 items)
        mockMvc.perform(get("/stats/")
                        .param("symbol", "AAPL")
                        .param("k", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avg").value(5.5))
                .andExpect(jsonPath("$.variance").value(8.25))
                .andExpect(jsonPath("$.min").value(1.0))
                .andExpect(jsonPath("$.max").value(10.0))
                .andExpect(jsonPath("$.last").value(10.0));
    }
}

