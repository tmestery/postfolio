package com.postfolio.postfolio.stockInvestmentAgents.capital;

import com.postfolio.postfolio.stockInvestmentAgents.model.Candidate;
import com.postfolio.postfolio.stockInvestmentAgents.model.RunResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Owns the allowance and runs the capital committee end to end
 * (docs/agent-trader-v2.md §6): three competing allocators → Cash Guard →
 * Capital Judge → Position Sizer → Risk/Book Gate. Appends every step to the
 * run trace and fills the capital fields on the RunResult.
 */
@Service
public class CapitalSupervisor {

    private final AggressiveAllocator aggressive;
    private final BalancedAllocator balanced;
    private final DefensiveAllocator defensive;
    private final CashGuard cashGuard;
    private final CapitalJudgeAgent capitalJudge;
    private final PositionSizer sizer;
    private final RiskBookGate riskGate;

    public CapitalSupervisor(AggressiveAllocator aggressive, BalancedAllocator balanced,
                             DefensiveAllocator defensive, CashGuard cashGuard,
                             CapitalJudgeAgent capitalJudge, PositionSizer sizer, RiskBookGate riskGate) {
        this.aggressive = aggressive;
        this.balanced = balanced;
        this.defensive = defensive;
        this.cashGuard = cashGuard;
        this.capitalJudge = capitalJudge;
        this.sizer = sizer;
        this.riskGate = riskGate;
    }

    /** Runs the committee; result.plannedShares holds the approved paper book. */
    public void allocate(RunResult result, List<Candidate> winners, Map<String, Double> quotes) {
        double allowance = result.startingAllowance;
        double reserve = result.cashReserveTarget;
        double deployable = allowance - reserve;

        List<AllocationProposal> proposals = List.of(
                aggressive.propose(winners, deployable, quotes),
                balanced.propose(winners, deployable, quotes),
                defensive.propose(winners, deployable, quotes));
        for (AllocationProposal p : proposals) {
            result.allocatorProposals.put(p.style, p.proposal);
            result.addTrace("allocator_" + p.style, "ok",
                    "Proposed %d positions".formatted(p.proposal.size()),
                    Map.of("proposal", p.proposal, "argument", p.argument));
        }

        Map<String, Object> guardReport = cashGuard.review(proposals, allowance, reserve);
        result.cashGuard = guardReport;
        result.addTrace("cash_guard", "ok",
                "Reviewed proposals against the $%.0f reserve floor".formatted(reserve), guardReport);

        Map<String, Object> decision = capitalJudge.decide(winners, proposals, guardReport, allowance, reserve);
        result.capitalJudgeDecision = decision;
        result.addTrace("capital_judge", "ok",
                "Approved book (%s style)".formatted(decision.get("winnerStyle")), decision);

        @SuppressWarnings("unchecked")
        Map<String, Double> approved = (Map<String, Double>) decision.get("approved");
        PositionSizer.Sizing sizing = sizer.size(approved, quotes);
        result.rejectedAllocations.addAll(sizing.rejected());
        result.addTrace("position_sizer", "ok",
                "Sized %d positions, %d skipped".formatted(sizing.shares().size(), sizing.rejected().size()),
                Map.of("shares", sizing.shares(), "skipped", sizing.rejected()));

        RiskBookGate.GateResult gate = riskGate.check(sizing.shares(), quotes, allowance, reserve);
        for (Map<String, Object> adjustment : gate.adjustments()) {
            if ("dropped".equals(adjustment.get("action"))) {
                result.rejectedAllocations.add(adjustment);
            }
        }
        result.plannedShares = gate.approvedShares();
        result.addTrace("risk_gate", gate.violations().isEmpty() ? "ok" : "adjusted",
                gate.violations().isEmpty()
                        ? "Book passed all hard rules"
                        : "Fixed %d violation(s) in one rebalance round".formatted(gate.violations().size()),
                Map.of("violations", gate.violations(), "adjustments", gate.adjustments(),
                        "approvedShares", gate.approvedShares()));
    }
}
