package com.postfolio.postfolio.stockInvestmentAgents.portfolio;

import com.postfolio.postfolio.stockInvestmentAgents.marketdata.AlpacaConfig;
import com.postfolio.postfolio.stockInvestmentAgents.marketdata.AlpacaRestClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class MarkToMarketService {

    private final PortfolioService portfolioService;
    private final AlpacaRestClient alpaca;
    private final AlpacaConfig alpacaConfig;

    public MarkToMarketService(PortfolioService portfolioService,
                               AlpacaRestClient alpaca,
                               AlpacaConfig alpacaConfig) {
        this.portfolioService = portfolioService;
        this.alpaca = alpaca;
        this.alpacaConfig = alpacaConfig;
    }

    @Transactional
    public PortfolioSnapshot refresh() {
        PortfolioSnapshot before = portfolioService.snapshot();
        List<String> tickers = before.positions.stream().map(p -> p.ticker).toList();
        if (tickers.isEmpty()) {
            return before;
        }
        Map<String, Double> marks = alpaca.fetchLatestMarks(tickers);
        portfolioService.updateMarks(marks);
        PortfolioSnapshot after = portfolioService.snapshot();
        after.marksStale = marks.isEmpty() || marks.size() < tickers.size();
        after.dataFeed = alpacaConfig.getDataFeed();
        if (!marks.isEmpty()) {
            portfolioService.recordMark(after);
        }
        return after;
    }
}
