package com.postfolio.postfolio.stockInvestmentAgents.capital;

import java.util.LinkedHashMap;
import java.util.Map;

/** One allocator's proposed book of dollar notionals. */
public class AllocationProposal {
    public String style;
    public Map<String, Double> proposal = new LinkedHashMap<>();
    public String argument;

    public AllocationProposal() {}

    public AllocationProposal(String style, Map<String, Double> proposal, String argument) {
        this.style = style;
        this.proposal = proposal;
        this.argument = argument;
    }
}
