package com.postfolio.postfolio.longTermHoldingInvestmentAgents;

import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;

// Going to utilize Marketaux to get information about specific stocks
public class analyzerAgent {
    private String api_key = System.getenv("MARKETAUX_API_KEY");

}