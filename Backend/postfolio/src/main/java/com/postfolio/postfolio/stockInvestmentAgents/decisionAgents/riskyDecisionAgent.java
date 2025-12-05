package com.postfolio.postfolio.stockInvestmentAgents;

import org.springframework.stereotype.Service;

// Going to utilize Marketaux to get information about specific stocks (price + other info)
@Service
public class riskyDecisionAgent {
    private String api_key = System.getenv("MARKETAUX_API_KEY");

}