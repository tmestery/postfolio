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
        while (allowance >= 0) {
            // Set up agent objects
            dataCollectorAgent dataAgent = new dataCollectorAgent();
            analyzerAgent analyzeAgent = new analyzerAgent();
            sentimentAnalyzerAgent sentimentAnalyzeAgent = new sentimentAnalyzerAgent();

            // Collect lists from agents
            List<String> marketNewsList = dataAgent.fetchGeneralMarketNews();
            if (marketNewsList != null) {
                for (String news : marketNewsList) {
                    System.out.println(news);
                }

                // Analyze all the data collected with Ollama
                String bestStockOptionname = sentimentAnalyzeAgent.sentimentAnalyzer(marketNewsList);

                // Add to stockList
                stockList.add(bestStockOptionname);
            }

            // Update allowance
            allowance -= 500;
        }

        return stockList;
    }
}