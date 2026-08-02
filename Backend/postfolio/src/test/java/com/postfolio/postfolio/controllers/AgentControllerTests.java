package com.postfolio.postfolio.controllers;

import com.postfolio.postfolio.stockInvestmentAgents.AgentUnavailableException;
import com.postfolio.postfolio.stockInvestmentAgents.managerAgents.executeAgent;
import com.postfolio.postfolio.stockInvestmentAgents.managerAgents.manager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AgentControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private manager manage;

    @MockitoBean
    private executeAgent execute;

    @Test
    void researchReturnsPicksWhenPipelineSucceeds() throws Exception {
        when(manage.deployAgents()).thenReturn(Map.of("NVDA", 3.0));

        mockMvc.perform(get("/trade/stock/test/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.NVDA").value(3.0));
    }

    @Test
    void researchReturns503WhenFinnhubKeyMissing() throws Exception {
        when(manage.deployAgents())
                .thenThrow(new AgentUnavailableException("FINNHUB_API_KEY is not configured on the server"));

        mockMvc.perform(get("/trade/stock/test/"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("FINNHUB_API_KEY is not configured on the server"));
    }

    @Test
    void researchReturns503WhenOllamaIsDown() throws Exception {
        when(manage.deployAgents())
                .thenThrow(new AgentUnavailableException("Ollama is not reachable at localhost:11434 (embeddings)"));

        mockMvc.perform(get("/trade/stock/test/"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Ollama is not reachable at localhost:11434 (embeddings)"));
    }

    @Test
    void executeReturns503WhenPipelineUnavailable() throws Exception {
        when(manage.deployAgents())
                .thenThrow(new AgentUnavailableException("Could not fetch market news from Finnhub"));

        mockMvc.perform(get("/trade/stock/execute/"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Could not fetch market news from Finnhub"));
    }

    @Test
    void executeReturnsTradeSummaryWhenPipelineSucceeds() throws Exception {
        when(manage.deployAgents()).thenReturn(Map.of("AAPL", 2.0));
        when(execute.executeTrades(anyMap(), anyDouble())).thenReturn(Map.of(
                "executedTrades", Map.of("AAPL", Map.of("shares", 2.0, "price", 200.0, "cost", 400.0)),
                "totalInvested", 400.0,
                "remainingAllowance", 600.0));

        mockMvc.perform(get("/trade/stock/execute/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInvested").value(400.0))
                .andExpect(jsonPath("$.executedTrades.AAPL.shares").value(2.0));
    }
}
