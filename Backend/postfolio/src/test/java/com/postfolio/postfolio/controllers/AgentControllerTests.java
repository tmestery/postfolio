package com.postfolio.postfolio.controllers;

import com.postfolio.postfolio.models.agentrun.AgentRun;
import com.postfolio.postfolio.models.agentrun.AgentRunRepository;
import com.postfolio.postfolio.stockInvestmentAgents.AgentUnavailableException;
import com.postfolio.postfolio.stockInvestmentAgents.model.RunResult;
import com.postfolio.postfolio.stockInvestmentAgents.supervisor.RunSupervisor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AgentControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgentRunRepository runRepository;

    @MockitoBean
    private RunSupervisor supervisor;

    private static RunResult sampleResult() {
        RunResult result = new RunResult();
        result.runId = UUID.randomUUID().toString();
        result.status = "completed";
        result.startingAllowance = 1000;
        result.cashReserveTarget = 150;
        result.totalInvested = 240;
        result.remainingAllowance = 760;
        result.executedTrades.put("NVDA", Map.of("shares", 2.0, "price", 120.0, "cost", 240.0));
        result.addTrace("news_scout", "ok", "Fetched 60 headlines", Map.of());
        result.addTrace("executor", "ok", "Filled 1 position(s)", Map.of());
        return result;
    }

    // Positive: execute returns the full RunResult DTO with trace.
    @Test
    void executeReturnsRunResultWithTrace() throws Exception {
        when(supervisor.run(eq(true), any())).thenReturn(sampleResult());

        mockMvc.perform(get("/trade/stock/execute/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.executedTrades.NVDA.cost").value(240.0))
                .andExpect(jsonPath("$.agentTrace[0].agent").value("news_scout"))
                .andExpect(jsonPath("$.agentTrace[1].agent").value("executor"));
    }

    // Negative: missing provider key surfaces as 503 with a JSON error body.
    @Test
    void missingDependencyReturns503() throws Exception {
        when(supervisor.run(eq(true), any()))
                .thenThrow(new AgentUnavailableException("GROQ_API_KEY is not configured on the server"));

        mockMvc.perform(get("/trade/stock/execute/"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("GROQ_API_KEY is not configured on the server"));
    }

    // Negative: research path never triggers fills (execute=false wiring).
    @Test
    void researchPathUsesPaperBookOnly() throws Exception {
        RunResult paper = sampleResult();
        paper.executedTrades.clear();
        paper.totalInvested = 0;
        paper.remainingAllowance = 1000;
        when(supervisor.run(eq(false), any())).thenReturn(paper);

        mockMvc.perform(get("/trade/stock/test/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executedTrades").isEmpty())
                .andExpect(jsonPath("$.remainingAllowance").value(1000.0));
    }

    // Negative: empty history returns an empty list, not an error.
    @Test
    void runsListEmptyWhenNoHistory() throws Exception {
        mockMvc.perform(get("/trade/runs/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // Positive: persisted runs come back as summaries.
    @Test
    void runsListReturnsSavedSummaries() throws Exception {
        AgentRun run = new AgentRun();
        run.setId(UUID.randomUUID());
        run.setUsername("demo");
        run.setStatus("completed");
        run.setStartingAllowance(1000);
        run.setTotalInvested(240);
        run.setRemainingAllowance(760);
        run.setResultJson("{}");
        runRepository.save(run);

        mockMvc.perform(get("/trade/runs/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("completed"))
                .andExpect(jsonPath("$[0].username").value("demo"))
                .andExpect(jsonPath("$[0].resultJson").doesNotExist());
    }

    @Test
    void getRunReturnsPersistedRunResult() throws Exception {
        UUID id = UUID.randomUUID();
        AgentRun run = new AgentRun();
        run.setId(id);
        run.setStatus("completed");
        run.setResultJson("""
                {"runId":"%s","status":"completed","agentTrace":[{"step":1,"agent":"news_scout","status":"ok","summary":"Fetched 3"}]}
                """.formatted(id));
        runRepository.save(run);

        mockMvc.perform(get("/trade/runs/" + id + "/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.agentTrace[0].agent").value("news_scout"));
    }

    @Test
    void getRunUnknownIdReturns404() throws Exception {
        mockMvc.perform(get("/trade/runs/" + UUID.randomUUID() + "/"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("run not found"));
    }

    @Test
    void getRunInvalidIdReturns400() throws Exception {
        mockMvc.perform(get("/trade/runs/not-a-uuid/"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid run id"));
    }

    @Test
    void getRunMissingResultJsonReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        AgentRun run = new AgentRun();
        run.setId(id);
        run.setStatus("failed");
        run.setResultJson("");
        runRepository.save(run);

        mockMvc.perform(get("/trade/runs/" + id + "/"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("run result missing"));
    }
}
