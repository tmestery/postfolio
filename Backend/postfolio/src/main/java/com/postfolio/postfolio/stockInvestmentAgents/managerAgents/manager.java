package com.postfolio.postfolio.stockInvestmentAgents.managerAgents;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.springframework.stereotype.Service;
import com.postfolio.postfolio.stockInvestmentAgents.analyzerAgents.costAnalysisAgent;
import com.postfolio.postfolio.stockInvestmentAgents.analyzerAgents.dataAnalyzerAgent;
import com.postfolio.postfolio.stockInvestmentAgents.rag.EmbeddingService;
import com.postfolio.postfolio.stockInvestmentAgents.rag.VectorStore;
import com.postfolio.postfolio.stockInvestmentAgents.rag.RetrievedContext;

@Service
public class manager {

    private double allowance = 1000;
    private List<String> stockList = new ArrayList<>();
    private List<Double> stockSharesAmount = new ArrayList<>();

    public Map<String, Double> deployAgents() {

        // Core agents
        dataCollection dataAgent = new dataCollection();
        dataAnalyzerAgent sentimentAnalyzeAgent = new dataAnalyzerAgent();
        costAnalysisAgent costAnalyzeAgent = new costAnalysisAgent();

        // RAG components
        EmbeddingService embeddingService = new EmbeddingService();
        VectorStore vectorStore = new VectorStore();

        // Load vectors on start up
        vectorStore.loadFromDisk();

        // Collect market news
        List<List<String>> marketNewsGrouped = dataAgent.fetchGeneralMarketNews();

        // Embed + store news in vector Database
        for (List<String> chunk : marketNewsGrouped) {
            for (String document : chunk) {
                vectorStore.add(
                        embeddingService.embed(document),
                        document
                );
            }
        }

        // Save new vectors
        vectorStore.saveToDisk();

        // Agent trades until out of money
        while (allowance > 0) {

            // Retrieval query - RAG trigger
            float[] queryEmbedding = embeddingService.embed(
                    "Identify companies with strong growth potential and positive market sentiment"
            );

            RetrievedContext context = new RetrievedContext(
                    vectorStore.search(queryEmbedding, 25)
            );

            // LLM analysis
            String bestStockOption = sentimentAnalyzeAgent.sentimentAnalyzer(
                    context.getDocuments(),
                    context.getDocuments().size(),
                    stockList
            );

            // Add new stock if not invested already
            if (!stockList.contains(bestStockOption)) {
                stockList.add(bestStockOption);
            }

            // Determine number of shares
            double shares = costAnalyzeAgent.determineNumShares(bestStockOption, allowance);
            stockSharesAmount.add(shares);

            // Update allowance
            allowance = costAnalyzeAgent.updateAllowance(allowance);

            // Safety break (avoid infinite loop)
            if (shares <= 0) break;
        }

        // Build result map
        Map<String, Double> result = new HashMap<>();
        for (int i = 0; i < stockList.size(); i++) {
            result.put(stockList.get(i), stockSharesAmount.get(i));
        }

        return result;
    }
}