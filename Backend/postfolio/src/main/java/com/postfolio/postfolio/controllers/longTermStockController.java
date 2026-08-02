package com.postfolio.postfolio.controllers;

import com.postfolio.postfolio.stockInvestmentAgents.AgentUnavailableException;
import com.postfolio.postfolio.stockInvestmentAgents.managerAgents.manager;
import com.postfolio.postfolio.stockInvestmentAgents.managerAgents.executeAgent;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/trade")
public class longTermStockController {
    private final manager manage;
    private final executeAgent execute;

    public longTermStockController(manager manage, executeAgent execute) {
        this.manage = manage;
        this.execute = execute;
    }

    /**
     * GET http://localhost:8080/trade/stock/test/
     *
     * Runs the research pipeline (Finnhub news + Ollama RAG analysis).
     *
     * @return 200 + map of ticker -> shares, or 503 when a dependency is down
     */
    @GetMapping("/stock/test/")
    public Map<String, Double> agentStockInvesting() {
        long start = System.currentTimeMillis();
        Map<String, Double> result = manage.deployAgents();
        long duration = System.currentTimeMillis() - start;
        System.out.println("Agent workflow completed in " + duration + " ms");
        return result;
    }

    /**
     * GET http://localhost:8080/trade/stock/execute/
     *
     * @return 200 + executed trades (shares, price, cost) plus allowance
     *         totals, or 503 when a dependency is down
     */
    @GetMapping("/stock/execute/")
    public Map<String, Object> executeTrades() {
        Map<String, Double> decisions = manage.deployAgents();
        return execute.executeTrades(decisions, 1000);
    }

    @ExceptionHandler(AgentUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleAgentUnavailable(AgentUnavailableException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", e.getMessage()));
    }
}
