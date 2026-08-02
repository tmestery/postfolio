package com.postfolio.postfolio.stockInvestmentAgents.marketdata;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Alpaca Market Data settings — Basic plan: IEX feed, 200 REST calls/min (docs/agent-trader-v4.md). */
@Component
public class AlpacaConfig {

    private final String apiKeyId;
    private final String apiSecretKey;
    private final String dataFeed;
    private final String dataBaseUrl;

    public AlpacaConfig(
            @Value("${ALPACA_API_KEY_ID:}") String apiKeyId,
            @Value("${ALPACA_API_SECRET_KEY:}") String apiSecretKey,
            @Value("${ALPACA_DATA_FEED:iex}") String dataFeed,
            @Value("${ALPACA_DATA_BASE_URL:https://data.alpaca.markets}") String dataBaseUrl) {
        this.apiKeyId = apiKeyId;
        this.apiSecretKey = apiSecretKey;
        this.dataFeed = dataFeed;
        this.dataBaseUrl = dataBaseUrl;
    }

    public boolean isConfigured() {
        return apiKeyId != null && !apiKeyId.isBlank()
                && apiSecretKey != null && !apiSecretKey.isBlank();
    }

    public String getApiKeyId() { return apiKeyId; }
    public String getApiSecretKey() { return apiSecretKey; }
    public String getDataFeed() { return dataFeed; }
    public String getDataBaseUrl() { return dataBaseUrl; }
}
