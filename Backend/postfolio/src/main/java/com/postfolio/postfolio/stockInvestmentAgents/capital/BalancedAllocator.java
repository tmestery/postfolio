package com.postfolio.postfolio.stockInvestmentAgents.capital;

import com.postfolio.postfolio.stockInvestmentAgents.groq.GroqClient;
import com.postfolio.postfolio.stockInvestmentAgents.groq.GroqConfig;
import org.springframework.stereotype.Service;

@Service
public class BalancedAllocator extends AbstractAllocator {

    public BalancedAllocator(GroqClient groq, GroqConfig config) {
        super(groq, config);
    }

    @Override
    public String style() {
        return "balanced";
    }

    @Override
    protected String biasPrompt() {
        return "Diversify across 3-5 names, weighting proportional to the judge scores. "
                + "No single name should exceed roughly 35% of the book.";
    }
}
