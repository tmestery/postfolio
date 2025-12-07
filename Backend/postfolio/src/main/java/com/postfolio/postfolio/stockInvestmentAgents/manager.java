package com.postfolio.postfolio.stockInvestmentAgents;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.postfolio.postfolio.stockInvestmentAgents.analyzerAgents.costAnalysisAgent;
import com.postfolio.postfolio.stockInvestmentAgents.analyzerAgents.dataAnalyzerAgent;

@Service
public class manager {
    private double allowance = 1000;
    private List<String> stockList = new ArrayList<>();
    private List<Double> stockSharesAmount = new ArrayList<>();

    public List<String> deployAgents() {
        // Set up agent objects
        dataCollection dataAgent = new dataCollection();
        dataAnalyzerAgent sentimentAnalyzeAgent = new dataAnalyzerAgent();
        costAnalysisAgent costAnalyzeAgent = new costAnalysisAgent();

        while (allowance >= 0) {
            // Collect lists from agents
            List<List<String>> marketNewsGrouped = dataAgent.fetchGeneralMarketNews();

            for (int i = 0; i < marketNewsGrouped.size(); i++) {

                List<String> chunk = marketNewsGrouped.get(i);

                // Run analyzer on 25 headlines at a time
                String bestStockOption = sentimentAnalyzeAgent.sentimentAnalyzer(
                        chunk,
                        chunk.size(),
                        stockList // pass already chosen ticker symbols
                );

                // Add to stockList if its not already on the list
                if (!stockList.contains(bestStockOption)) {
                    stockList.add(bestStockOption);
                }

                // Investment Shares Cost:
                stockSharesAmount.add(costAnalyzeAgent.determineNumShares(bestStockOption, allowance));
                System.out.println(costAnalyzeAgent.determineNumShares(bestStockOption, allowance));

                // Update allowance with agent advice
                allowance = costAnalyzeAgent.updateAllowance(allowance);
            }
        }

        return stockList;
    }
}