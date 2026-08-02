package com.postfolio.postfolio.stockInvestmentAgents.capital;

import com.postfolio.postfolio.stockInvestmentAgents.groq.GroqClient;
import com.postfolio.postfolio.stockInvestmentAgents.groq.GroqConfig;
import org.springframework.stereotype.Service;

@Service
public class DefensiveAllocator extends AbstractAllocator {

    public DefensiveAllocator(GroqClient groq, GroqConfig config) {
        super(groq, config);
    }

    @Override
    public String style() {
        return "defensive";
    }

    @Override
    protected String biasPrompt() {
        return "Take smaller positions in the highest-consensus, lowest-drama names. "
                + "If conviction is weak, deploy well under the available capital and hold extra cash.";
    }
}
