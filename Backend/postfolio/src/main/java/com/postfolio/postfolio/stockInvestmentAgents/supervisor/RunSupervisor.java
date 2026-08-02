package com.postfolio.postfolio.stockInvestmentAgents.supervisor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.postfolio.postfolio.models.agentrun.AgentRun;
import com.postfolio.postfolio.models.agentrun.AgentRunRepository;
import com.postfolio.postfolio.stockInvestmentAgents.AgentUnavailableException;
import com.postfolio.postfolio.stockInvestmentAgents.capital.CapitalSupervisor;
import com.postfolio.postfolio.stockInvestmentAgents.debate.BearAgent;
import com.postfolio.postfolio.stockInvestmentAgents.debate.BullAgent;
import com.postfolio.postfolio.stockInvestmentAgents.debate.StockJudgeAgent;
import com.postfolio.postfolio.stockInvestmentAgents.execute.TradeExecutor;
import com.postfolio.postfolio.stockInvestmentAgents.groq.GroqConfig;
import com.postfolio.postfolio.stockInvestmentAgents.model.Candidate;
import com.postfolio.postfolio.stockInvestmentAgents.model.RunResult;
import com.postfolio.postfolio.stockInvestmentAgents.portfolio.PortfolioService;
import com.postfolio.postfolio.stockInvestmentAgents.portfolio.PortfolioSnapshot;
import com.postfolio.postfolio.stockInvestmentAgents.research.ResearchSupervisor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates one agent run (docs/agent-trader-v3.md): web research → bull →
 * bear → stock judge → price scout → capital committee → (optionally) executor.
 */
@Service
public class RunSupervisor {

    private final GroqConfig config;
    private final ResearchSupervisor research;
    private final BullAgent bull;
    private final BearAgent bear;
    private final StockJudgeAgent stockJudge;
    private final CapitalSupervisor capital;
    private final TradeExecutor executor;
    private final AgentRunRepository runRepository;
    private final PortfolioService portfolioService;
    private final ObjectMapper mapper = new ObjectMapper();

    public RunSupervisor(GroqConfig config, ResearchSupervisor research,
                         BullAgent bull, BearAgent bear, StockJudgeAgent stockJudge,
                         CapitalSupervisor capital, TradeExecutor executor,
                         AgentRunRepository runRepository,
                         PortfolioService portfolioService) {
        this.config = config;
        this.research = research;
        this.bull = bull;
        this.bear = bear;
        this.stockJudge = stockJudge;
        this.capital = capital;
        this.executor = executor;
        this.runRepository = runRepository;
        this.portfolioService = portfolioService;
    }

    /**
     * @param execute false = paper book only (research path), true = simulated fills
     * @param username optional demo attribution stored with the run
     */
    public RunResult run(boolean execute, String username) {
        long startedAt = System.currentTimeMillis();
        RunResult result = new RunResult();
        result.runId = UUID.randomUUID().toString();
        result.status = "completed";
        if (execute) {
            double houseCash = portfolioService.getOrCreate().getCash();
            result.startingAllowance = houseCash;
            result.cashReserveTarget = round2(config.getAllowance() * config.getCashReservePct());
            result.remainingAllowance = houseCash;
            if (houseCash <= result.cashReserveTarget) {
                result.addTrace("supervisor", "stopped",
                        "House cash at or below the $%.0f reserve floor".formatted(result.cashReserveTarget),
                        Map.of("cash", houseCash));
                result.portfolio = portfolioService.snapshot().toSummary();
                return persist(result, username);
            }
        } else {
            result.startingAllowance = config.getAllowance();
            result.cashReserveTarget = round2(config.getAllowance() * config.getCashReservePct());
            result.remainingAllowance = result.startingAllowance;
        }

        try {
            List<String> headlines = research.gatherEvidence(result);
            if (timedOut(startedAt, result)) return persist(result, username);

            List<Candidate> candidates = bull.propose(headlines);
            result.addTrace("bull", "ok", "Proposed %d candidate(s)".formatted(candidates.size()),
                    bullDetail(candidates));
            if (candidates.isEmpty()) {
                result.addTrace("supervisor", "stopped", "Bull produced no valid candidates", Map.of());
                return persist(result, username);
            }
            if (timedOut(startedAt, result)) return persist(result, username);

            bear.critique(headlines, candidates);
            result.addTrace("bear", "ok", "Critiqued %d candidate(s)".formatted(candidates.size()),
                    bearDetail(candidates));
            if (timedOut(startedAt, result)) return persist(result, username);

            StockJudgeAgent.Verdict verdict = stockJudge.judge(candidates);
            result.candidates = verdict.advanced();
            result.rejectedTickers.addAll(verdict.rejected());
            result.addTrace("stock_judge", "ok",
                    "Advanced %d, rejected %d".formatted(verdict.advanced().size(), verdict.rejected().size()),
                    judgeDetail(verdict));
            if (verdict.advanced().isEmpty()) {
                result.addTrace("supervisor", "stopped", "No candidates advanced to capital", Map.of());
                return persist(result, username);
            }
            if (timedOut(startedAt, result)) return persist(result, username);

            result.quoteSnapshot = research.quoteSnapshot(
                    result, verdict.advanced().stream().map(c -> c.ticker).toList());
            List<Candidate> investable = verdict.advanced().stream()
                    .filter(c -> result.quoteSnapshot.containsKey(c.ticker))
                    .toList();
            for (Candidate c : verdict.advanced()) {
                if (!result.quoteSnapshot.containsKey(c.ticker)) {
                    result.rejectedTickers.add(Map.of("ticker", c.ticker, "reason", "no_valid_quote"));
                }
            }
            if (investable.isEmpty()) {
                result.addTrace("supervisor", "stopped", "No investable tickers with valid quotes", Map.of());
                return persist(result, username);
            }
            if (timedOut(startedAt, result)) return persist(result, username);

            capital.allocate(result, investable, result.quoteSnapshot);
            if (timedOut(startedAt, result)) return persist(result, username);

            if (execute) {
                executor.execute(result, result.quoteSnapshot);
                PortfolioSnapshot book = portfolioService.applyFills(result.executedTrades);
                result.portfolio = book.toSummary();
                result.remainingAllowance = book.cash;
            } else {
                result.addTrace("supervisor", "ok", "Research run — paper book only, no fills", Map.of());
            }
            return persist(result, username);
        } catch (AgentUnavailableException e) {
            persistFailure(result, username, e.getMessage());
            throw e;
        }
    }

    private boolean timedOut(long startedAt, RunResult result) {
        if (System.currentTimeMillis() - startedAt <= config.getTimeoutMs()) return false;
        result.status = "partial";
        result.error = "Run exceeded the %dms wall-clock budget; returning partial results"
                .formatted(config.getTimeoutMs());
        result.addTrace("supervisor", "timeout", "Wall-clock budget exhausted", Map.of());
        return true;
    }

    private RunResult persist(RunResult result, String username) {
        try {
            AgentRun run = new AgentRun();
            run.setId(UUID.fromString(result.runId));
            run.setUsername(username);
            run.setStatus(result.status);
            run.setStartingAllowance(result.startingAllowance);
            run.setTotalInvested(result.totalInvested);
            run.setRemainingAllowance(result.remainingAllowance);
            run.setError(result.error);
            run.setResultJson(mapper.writeValueAsString(result));
            runRepository.save(run);
        } catch (Exception e) {
            System.err.println("Could not persist agent run: " + e.getMessage());
        }
        return result;
    }

    private void persistFailure(RunResult result, String username, String error) {
        result.status = "failed";
        result.error = error;
        persist(result, username);
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static Map<String, Object> bullDetail(List<Candidate> candidates) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Candidate c : candidates) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("ticker", c.ticker);
            row.put("thesis", c.thesis == null ? "" : c.thesis);
            row.put("confidence", c.confidence);
            rows.add(row);
        }
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("tickers", candidates.stream().map(c -> c.ticker).toList());
        detail.put("candidates", rows);
        detail.put("provider", "groq");
        return detail;
    }

    private static Map<String, Object> bearDetail(List<Candidate> candidates) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Candidate c : candidates) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("ticker", c.ticker);
            row.put("risks", c.risks == null ? List.of() : c.risks);
            row.put("severityDown", c.severityDown);
            rows.add(row);
        }
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("risksFound", candidates.stream().mapToInt(c -> c.risks == null ? 0 : c.risks.size()).sum());
        detail.put("critiques", rows);
        detail.put("provider", "groq");
        return detail;
    }

    private static Map<String, Object> judgeDetail(StockJudgeAgent.Verdict verdict) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("advanced", verdict.advanced().stream().map(c -> c.ticker).toList());
        detail.put("rejected", verdict.rejected());
        detail.put("provider", "groq");
        return detail;
    }
}
