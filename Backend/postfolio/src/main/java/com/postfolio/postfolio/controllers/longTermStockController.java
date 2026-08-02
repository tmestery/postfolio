package com.postfolio.postfolio.controllers;

import com.postfolio.postfolio.models.agentrun.AgentRun;
import com.postfolio.postfolio.models.agentrun.AgentRunRepository;
import com.postfolio.postfolio.stockInvestmentAgents.AgentUnavailableException;
import com.postfolio.postfolio.stockInvestmentAgents.model.RunResult;
import com.postfolio.postfolio.stockInvestmentAgents.supervisor.RunSupervisor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Agent trader endpoints (docs/agent-trader-v2.md §8).
 * Both run endpoints return the full RunResult DTO with agentTrace.
 */
@RestController
@RequestMapping("/trade")
public class longTermStockController {

    private final RunSupervisor supervisor;
    private final AgentRunRepository runRepository;
    private final ObjectMapper mapper = new ObjectMapper();

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

    /** Full persisted RunResult for the agent desk history drill-in. */
    @GetMapping("/runs/{runId}/")
    public ResponseEntity<?> getRun(@PathVariable String runId) {
        UUID id;
        try {
            id = UUID.fromString(runId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid run id"));
        }
        Optional<AgentRun> found = runRepository.findById(id);
        if (found.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "run not found"));
        }
        String json = found.get().getResultJson();
        if (json == null || json.isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "run result missing"));
        }
        try {
            return ResponseEntity.ok(mapper.readValue(json, RunResult.class));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "could not parse stored run result"));
        }
    }

    @ExceptionHandler(AgentUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleAgentUnavailable(AgentUnavailableException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", e.getMessage()));
    }
}
