package com.postfolio.postfolio.longTermHoldingInvestmentAgents;

import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;

// Going to utilize Finnhub API to collect most recent market news
public class dataCollectorAgent {
    private String api_key = System.getenv("FINNHUB_API_KEY");

}