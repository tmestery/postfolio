package com.postfolio.postfolio.stockInvestmentAgents.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Versioned response DTO for one agent run (docs/agent-trader-v2.md §8).
 * Serialized as-is to the frontend and into agent_run.result_json.
 */
public class RunResult {
    public String runId;
    /** completed | partial | failed */
    public String status;
    public String error;

    public double startingAllowance;
    public double cashReserveTarget;
    public double totalInvested;
    public double remainingAllowance;

    public List<Candidate> candidates = new ArrayList<>();
    public List<Map<String, String>> rejectedTickers = new ArrayList<>();
    public Map<String, Double> quoteSnapshot = new LinkedHashMap<>();
    /** Scraped evidence for the desk (docs/agent-trader-v3.md). */
    public List<Map<String, Object>> evidencePack = new ArrayList<>();

    public Map<String, Map<String, Double>> allocatorProposals = new LinkedHashMap<>();
    public Map<String, Object> cashGuard = new LinkedHashMap<>();
    public Map<String, Object> capitalJudgeDecision = new LinkedHashMap<>();
    public List<Map<String, Object>> rejectedAllocations = new ArrayList<>();

    /** Paper book after sizing + risk gate ({ ticker → shares }). */
    public Map<String, Integer> plannedShares = new LinkedHashMap<>();
    /** Fills ({ ticker → { shares, price, cost } }); empty on research-only runs. */
    public Map<String, Map<String, Double>> executedTrades = new LinkedHashMap<>();
    /** House book summary after execute (docs/agent-trader-v4.md). */
    public Map<String, Object> portfolio = new LinkedHashMap<>();

    public List<TraceStep> agentTrace = new ArrayList<>();

    public void addTrace(String agent, String status, String summary, Map<String, Object> detail) {
        agentTrace.add(new TraceStep(agentTrace.size() + 1, agent, status, summary, detail));
    }
}
