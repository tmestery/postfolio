package com.postfolio.postfolio.longTermHoldingInvestmentAgents;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class manager {
    public String deployAgents() {
        // Set up agent objects
        dataCollectorAgent dataAgent = new dataCollectorAgent();
        analyzerAgent analyzeAgent = new analyzerAgent();
        sentimentAnalyzerAgent sentimentAnalyzeAgent = new sentimentAnalyzerAgent();

        // Collect lists from agents
        List<String> marketNewsList = dataAgent.fetchGeneralMarketNews();
        for (int i = 0; i < marketNewsList.size(); i++) {
            System.out.println(marketNewsList.get(i));
        }

        // Analyze all the data collected with Ollama
        String bestStockOptionname = sentimentAnalyzeAgent.sentimentAnalyzer(marketNewsList);

        return bestStockOptionname;
    }
}