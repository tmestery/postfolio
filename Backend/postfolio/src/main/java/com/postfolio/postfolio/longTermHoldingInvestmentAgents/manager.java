package com.postfolio.postfolio.longTermHoldingInvestmentAgents;

public class manager {
    public void deployAgents() {
        // Set up agent objects
        dataCollectorAgent dataAgent = new dataCollectorAgent();
        analyzerAgent analyzeAgent = new analyzerAgent();
        sentimentAnalyzerAgent sentimentAnalyzeAgent = new sentimentAnalyzerAgent();


    }
}