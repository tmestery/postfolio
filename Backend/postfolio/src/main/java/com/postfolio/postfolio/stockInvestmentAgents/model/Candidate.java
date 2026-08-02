package com.postfolio.postfolio.stockInvestmentAgents.model;

import java.util.ArrayList;
import java.util.List;

/** A ticker moving through bull → bear → judge. */
public class Candidate {
    public String ticker;
    public String thesis;
    public double confidence;
    public List<String> risks = new ArrayList<>();
    public double severityDown;
    public double score;
    public String decision;
    public String rationale;

    public Candidate() {}

    public Candidate(String ticker, String thesis, double confidence) {
        this.ticker = ticker;
        this.thesis = thesis;
        this.confidence = confidence;
    }
}
