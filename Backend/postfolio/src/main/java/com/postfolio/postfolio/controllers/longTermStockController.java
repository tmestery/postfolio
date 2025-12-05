package com.postfolio.postfolio.controllers;

import com.postfolio.postfolio.longTermHoldingInvestmentAgents.manager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/llm")
public class longTermStockController {
    private final manager manage;

    public longTermStockController(manager manage) {
        this.manage = manage;
    }

    // GET http://localhost:8080/llm/health/tip
    @GetMapping("/stock/longterm/")
    public String healthTipGeneration() {
        System.out.println("Initializing Agents!");
        System.out.println(manage.deployAgents());
        return manage.deployAgents();
    }
}