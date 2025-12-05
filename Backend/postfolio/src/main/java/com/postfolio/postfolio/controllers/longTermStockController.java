package com.postfolio.postfolio.controllers;

import com.postfolio.postfolio.longTermHoldingInvestmentAgents.manager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/trade")
public class longTermStockController {
    private final manager manage;

    public longTermStockController(manager manage) {
        this.manage = manage;
    }

    // GET http://localhost:8080/trade/stock/llm/
    @GetMapping("/stock/llm/")
    public List<String> healthTipGeneration() {
        System.out.println("Initializing Agents!");
        return manage.deployAgents();
    }
}