package com.postfolio.postfolio.controllers;

import com.postfolio.postfolio.stockInvestmentAgents.managerAgents.manager;
import com.postfolio.postfolio.stockInvestmentAgents.managerAgents.executeAgent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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
     * GET http://localhost:8080/trade/stock/llm/
     *
     * @return a map of stocks + shares
     */
    @GetMapping("/stock/test/")
    public Map<String, Double> agentStockInvesting() {
        System.out.println("Initializing Agents!");
        return manage.deployAgents();
    }

    /**
     * GET http://localhost:8080/trade/stock/execute/
     *
     * @return a map of stock + shares + cost + price paid as
     * well as allowance data (total invested, remaining allowance)
     */
    @GetMapping("/stock/execute/")
    public Map<String, Object> executeTrades() {
        Map<String, Double> decisions = manage.deployAgents();
        return execute.executeTrades(decisions, 1000);
    }
}