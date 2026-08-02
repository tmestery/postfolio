package com.postfolio.postfolio.stockInvestmentAgents.capital;

import com.postfolio.postfolio.stockInvestmentAgents.groq.GroqClient;
import com.postfolio.postfolio.stockInvestmentAgents.groq.GroqConfig;
import org.springframework.stereotype.Service;

@Service
public class AggressiveAllocator extends AbstractAllocator {

    public AggressiveAllocator(GroqClient groq, GroqConfig config) {
        super(groq, config);
    }

    @Override
    public String style() {
        return "aggressive";
    }

    @Override
    protected String biasPrompt() {
        return "Concentrate 40-60% of deployable capital into the single highest-conviction name; "
                + "put the remainder into the #2/#3 picks or leave as cash.";
    }
}
