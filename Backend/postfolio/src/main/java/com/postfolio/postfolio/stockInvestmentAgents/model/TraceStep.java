package com.postfolio.postfolio.stockInvestmentAgents.model;

import java.util.LinkedHashMap;
import java.util.Map;

/** One entry in the run's agentTrace timeline. */
public class TraceStep {
    public int step;
    public String agent;
    public String status;
    public String summary;
    public Map<String, Object> detail = new LinkedHashMap<>();

    public TraceStep() {}

    public TraceStep(int step, String agent, String status, String summary, Map<String, Object> detail) {
        this.step = step;
        this.agent = agent;
        this.status = status;
        this.summary = summary;
        if (detail != null) this.detail = detail;
    }
}
