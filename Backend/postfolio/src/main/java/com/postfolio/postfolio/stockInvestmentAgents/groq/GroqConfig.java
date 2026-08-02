package com.postfolio.postfolio.stockInvestmentAgents.groq;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Env-driven settings for the agent pipeline (see docs/agent-trader-v2.md §12). */
@Component
public class GroqConfig {

    private final String apiKey;
    private final String fastModel;
    private final String judgeModel;
    private final double allowance;
    private final double cashReservePct;
    private final long timeoutMs;

    public GroqConfig(
            @Value("${GROQ_API_KEY:}") String apiKey,
            @Value("${GROQ_MODEL_FAST:llama-3.1-8b-instant}") String fastModel,
            @Value("${GROQ_MODEL_JUDGE:llama-3.3-70b-versatile}") String judgeModel,
            @Value("${AGENT_ALLOWANCE:1000}") double allowance,
            @Value("${AGENT_CASH_RESERVE_PCT:0.15}") double cashReservePct,
            @Value("${AGENT_TIMEOUT_MS:60000}") long timeoutMs) {
        this.apiKey = apiKey;
        this.fastModel = fastModel;
        this.judgeModel = judgeModel;
        this.allowance = allowance;
        this.cashReservePct = cashReservePct;
        this.timeoutMs = timeoutMs;
    }

    public String getApiKey() { return apiKey; }
    public String getFastModel() { return fastModel; }
    public String getJudgeModel() { return judgeModel; }
    public double getAllowance() { return allowance; }
    public double getCashReservePct() { return cashReservePct; }
    public long getTimeoutMs() { return timeoutMs; }
}
