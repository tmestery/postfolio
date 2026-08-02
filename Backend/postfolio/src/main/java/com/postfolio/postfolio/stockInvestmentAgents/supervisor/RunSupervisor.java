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
import com.postfolio.postfolio.stockInvestmentAgents.news.NewsScout;
import com.postfolio.postfolio.stockInvestmentAgents.news.QuoteService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates one agent run (docs/agent-trader-v2.md §5.1): news → bull →
 * bear → stock judge → quote snapshot/investability gate → capital committee
 * → (optionally) executor. Owns the wall-clock timeout and persists the run.
 */
@Service
public class RunSupervisor {

    private final GroqConfig config;
    private final NewsScout newsScout;
    private final QuoteService quoteService;
    private final BullAgent bull;
    private final BearAgent bear;
    private final StockJudgeAgent stockJudge;
    private final CapitalSupervisor capital;
    private final TradeExecutor executor;
    private final AgentRunRepository runRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public RunSupervisor(GroqConfig config, NewsScout newsScout, QuoteService quoteService,
                         BullAgent bull, BearAgent bear, StockJudgeAgent stockJudge,
                         CapitalSupervisor capital, TradeExecutor executor,
                         AgentRunRepository runRepository) {
        this.config = config;
        this.newsScout = newsScout;
        this.quoteService = quoteService;
        this.bull = bull;
        this.bear = bear;
        this.stockJudge = stockJudge;
        this.capital = capital;
        this.executor = executor;
        this.runRepository = runRepository;
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
        result.startingAllowance = config.getAllowance();
        result.cashReserveTarget = round2(config.getAllowance() * config.getCashReservePct());
        result.remainingAllowance = result.startingAllowance;

        try {
            List<String> headlines = newsScout.fetchHeadlines();
            result.addTrace("news_scout", "ok", "Fetched %d headlines".formatted(headlines.size()),
                    Map.of("sample", headlines.subList(0, Math.min(5, headlines.size()))));
            if (timedOut(startedAt, result)) return persist(result, username);

            List<Candidate> candidates = bull.propose(headlines);
            result.addTrace("bull", "ok", "Proposed %d candidate(s)".formatted(candidates.size()),
                    Map.of("tickers", candidates.stream().map(c -> c.ticker).toList()));
            if (candidates.isEmpty()) {
                result.addTrace("supervisor", "stopped", "Bull produced no valid candidates", Map.of());
                return persist(result, username);
            }
            if (timedOut(startedAt, result)) return persist(result, username);

            bear.critique(headlines, candidates);
            result.addTrace("bear", "ok", "Critiqued %d candidate(s)".formatted(candidates.size()),
                    Map.of("risksFound", candidates.stream().mapToInt(c -> c.risks.size()).sum()));
            if (timedOut(startedAt, result)) return persist(result, username);

            StockJudgeAgent.Verdict verdict = stockJudge.judge(candidates);
            result.candidates = verdict.advanced();
            result.rejectedTickers.addAll(verdict.rejected());
            result.addTrace("stock_judge", "ok",
                    "Advanced %d, rejected %d".formatted(verdict.advanced().size(), verdict.rejected().size()),
                    Map.of("advanced", verdict.advanced().stream().map(c -> c.ticker).toList()));
            if (verdict.advanced().isEmpty()) {
                result.addTrace("supervisor", "stopped", "No candidates advanced to capital", Map.of());
                return persist(result, username);
            }
            if (timedOut(startedAt, result)) return persist(result, username);

            // §5.5b — one quote snapshot per run + investability gate.
            result.quoteSnapshot = quoteService.snapshot(
                    verdict.advanced().stream().map(c -> c.ticker).toList());
            List<Candidate> investable = verdict.advanced().stream()
                    .filter(c -> result.quoteSnapshot.containsKey(c.ticker))
                    .toList();
            for (Candidate c : verdict.advanced()) {
                if (!result.quoteSnapshot.containsKey(c.ticker)) {
                    result.rejectedTickers.add(Map.of("ticker", c.ticker, "reason", "no_valid_quote"));
                }
            }
            result.addTrace("quote_snapshot", "ok",
                    "Quoted %d of %d tickers".formatted(investable.size(), verdict.advanced().size()),
                    Map.of("quotes", result.quoteSnapshot));
            if (investable.isEmpty()) {
                result.addTrace("supervisor", "stopped", "No investable tickers with valid quotes", Map.of());
                return persist(result, username);
            }
            if (timedOut(startedAt, result)) return persist(result, username);

            capital.allocate(result, investable, result.quoteSnapshot);
            if (timedOut(startedAt, result)) return persist(result, username);

            if (execute) {
                executor.execute(result, result.quoteSnapshot);
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
            // History is a nice-to-have; never fail a demo run over it.
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
}
