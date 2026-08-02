package com.postfolio.postfolio.controllers;

import com.postfolio.postfolio.models.agentrun.AgentRunRepository;
import com.postfolio.postfolio.stockInvestmentAgents.AgentUnavailableException;
import com.postfolio.postfolio.stockInvestmentAgents.model.RunResult;
import com.postfolio.postfolio.stockInvestmentAgents.supervisor.RunSupervisor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent trader endpoints (docs/agent-trader-v2.md §8).
 * Both run endpoints return the full RunResult DTO with agentTrace.
 */
@RestController
@RequestMapping("/trade")
public class longTermStockController {

    private final RunSupervisor supervisor;
    private final AgentRunRepository runRepository;

    public longTermStockController(RunSupervisor supervisor, AgentRunRepository runRepository) {
        this.supervisor = supervisor;
        this.runRepository = runRepository;
    }

    /** Research run: full pipeline through Risk Gate, paper book only (no fills). */
    @GetMapping("/stock/test/")
    public RunResult research(@RequestParam(required = false) String username) {
        return supervisor.run(false, username);
    }

    /** Full run including simulated fills. */
    @GetMapping("/stock/execute/")
    public RunResult execute(@RequestParam(required = false) String username) {
        return supervisor.run(true, username);
    }

    /** Recent run history (summaries; full DTO lives in result_json). */
    @GetMapping("/runs/")
    public List<Map<String, Object>> recentRuns() {
        return runRepository.findTop20ByOrderByCreatedAtDesc().stream()
                .map(run -> {
                    Map<String, Object> summary = new LinkedHashMap<>();
                    summary.put("runId", run.getId().toString());
                    summary.put("createdAt", run.getCreatedAt().toString());
                    summary.put("username", run.getUsername());
                    summary.put("status", run.getStatus());
                    summary.put("startingAllowance", run.getStartingAllowance());
                    summary.put("totalInvested", run.getTotalInvested());
                    summary.put("remainingAllowance", run.getRemainingAllowance());
                    summary.put("error", run.getError());
                    return summary;
                })
                .toList();
    }

    @ExceptionHandler(AgentUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleAgentUnavailable(AgentUnavailableException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", e.getMessage()));
    }
}
