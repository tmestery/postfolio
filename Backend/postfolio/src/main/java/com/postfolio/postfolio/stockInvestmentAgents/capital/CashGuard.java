package com.postfolio.postfolio.stockInvestmentAgents.capital;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Opposes over-deployment with hard code rules (docs/agent-trader-v2.md §6.6).
 * Reviews each allocator proposal against the cash reserve floor and vetoes
 * proposals whose spend would breach it.
 */
@Service
public class CashGuard {

    public Map<String, Object> review(List<AllocationProposal> proposals,
                                      double startingAllowance, double reserveTarget) {
        double deployable = startingAllowance - reserveTarget;
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("reserveFloor", reserveTarget);
        report.put("deployableCapital", deployable);

        List<Map<String, Object>> reviews = new ArrayList<>();
        for (AllocationProposal p : proposals) {
            double spend = p.proposal.values().stream().mapToDouble(Double::doubleValue).sum();
            boolean veto = spend > deployable;
            String note;
            if (veto) {
                note = "VETO: proposal spends $%.2f which breaches the $%.2f cash reserve floor".formatted(spend, reserveTarget);
            } else if (spend > deployable * 0.95) {
                note = "Caution: proposal skims the reserve floor with only $%.2f headroom".formatted(deployable - spend);
            } else {
                note = "OK: $%.2f deployed, $%.2f held back above the reserve".formatted(spend, deployable - spend);
            }
            reviews.add(Map.of("style", p.style, "proposedSpend", spend, "vetoSpend", veto, "note", note));
        }
        report.put("reviews", reviews);
        return report;
    }
}
