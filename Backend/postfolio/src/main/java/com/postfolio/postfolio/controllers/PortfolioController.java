package com.postfolio.postfolio.controllers;

import com.postfolio.postfolio.stockInvestmentAgents.marketdata.AlpacaConfig;
import com.postfolio.postfolio.stockInvestmentAgents.portfolio.MarkToMarketService;
import com.postfolio.postfolio.stockInvestmentAgents.portfolio.PortfolioService;
import com.postfolio.postfolio.stockInvestmentAgents.portfolio.PortfolioSnapshot;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** House paper portfolio — simulated book, Alpaca marks only (docs/agent-trader-v4.md). */
@RestController
@RequestMapping("/trade/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final MarkToMarketService markToMarket;
    private final AlpacaConfig alpacaConfig;

    public PortfolioController(PortfolioService portfolioService,
                               MarkToMarketService markToMarket,
                               AlpacaConfig alpacaConfig) {
        this.portfolioService = portfolioService;
        this.markToMarket = markToMarket;
        this.alpacaConfig = alpacaConfig;
    }

    @GetMapping("/")
    public PortfolioSnapshot getPortfolio() {
        PortfolioSnapshot snap = portfolioService.snapshot();
        snap.dataFeed = alpacaConfig.getDataFeed();
        return snap;
    }

    @GetMapping("/history/")
    public Map<String, Object> history() {
        List<Map<String, Object>> points = portfolioService.historyPoints();
        if (points.isEmpty()) {
            PortfolioSnapshot snap = portfolioService.snapshot();
            Map<String, Object> seed = new LinkedHashMap<>();
            seed.put("t", snap.asOf.toString());
            seed.put("equity", snap.equity);
            seed.put("totalPnl", snap.totalPnl);
            points = List.of(seed);
        }
        return Map.of("points", points);
    }

    @PostMapping("/refresh/")
    public ResponseEntity<?> refresh() {
        if (!alpacaConfig.isConfigured()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Alpaca API keys not configured"));
        }
        PortfolioSnapshot snap = markToMarket.refresh();
        if (snap.positions.isEmpty()) {
            return ResponseEntity.ok(snap);
        }
        if (snap.marksStale && snap.positions.stream().allMatch(p -> p.markPrice == null)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Could not fetch marks from Alpaca"));
        }
        return ResponseEntity.ok(snap);
    }

    @PostMapping("/reset/")
    public PortfolioSnapshot reset() {
        return portfolioService.reset();
    }
}
