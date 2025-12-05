package com.postfolio.postfolio.longTermHoldingInvestmentAgents;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

@Service
public class manager {
    private int allowance = 1000;
    private List<String> stockList = new ArrayList<>();

    public List<String> deployAgents() {
        // Set up agent objects
        dataCollectorAgent dataAgent = new dataCollectorAgent();
        analyzerAgent analyzeAgent = new analyzerAgent();
        sentimentAnalyzerAgent sentimentAnalyzeAgent = new sentimentAnalyzerAgent();

        while (allowance >= 0) {
            // Collect lists from agents
            // NOW returns List<List<String>>
            List<List<String>> marketNewsGrouped = dataAgent.fetchGeneralMarketNews();

            for (int i = 0; i < marketNewsGrouped.size(); i++) {

                List<String> chunk = marketNewsGrouped.get(i);   // List<String>

                // Run sentiment on ONE chunk (25 headlines)
                String bestStockOption = sentimentAnalyzeAgent.sentimentAnalyzer(
                        chunk,
                        chunk.size(),
                        stockList // pass previously chosen tickers
                );

                // Add to stockList if its not already on the list
                if (!stockList.contains(bestStockOption)) {
                    stockList.add(bestStockOption);
                }

                // Update allowance with agent advice
                allowance -= 250;
            }
        }

        return stockList;
    }
}