package com.postfolio.postfolio.stockInvestmentAgents.supervisor;

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
import com.postfolio.postfolio.stockInvestmentAgents.model.TraceStep;
import com.postfolio.postfolio.stockInvestmentAgents.news.NewsScout;
import com.postfolio.postfolio.stockInvestmentAgents.news.QuoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RunSupervisorTests {

    private NewsScout newsScout;
    private QuoteService quoteService;
    private BullAgent bull;
    private BearAgent bear;
    private StockJudgeAgent judge;
    private CapitalSupervisor capital;
    private AgentRunRepository runRepository;

    private RunSupervisor supervisor(long timeoutMs) {
        GroqConfig config = new GroqConfig("key", "fast", "judge", 1000, 0.15, timeoutMs);
        return new RunSupervisor(config, newsScout, quoteService, bull, bear, judge,
                capital, new TradeExecutor(), runRepository);
    }

    @BeforeEach
    void setUp() {
        newsScout = mock(NewsScout.class);
        quoteService = mock(QuoteService.class);
        bull = mock(BullAgent.class);
        bear = mock(BearAgent.class);
        judge = mock(StockJudgeAgent.class);
        capital = mock(CapitalSupervisor.class);
        runRepository = mock(AgentRunRepository.class);
    }

    private void stubHappyPath() {
        Candidate nvda = new Candidate("NVDA", "AI demand", 0.8);
        nvda.score = 8.0;
        when(newsScout.fetchHeadlines()).thenReturn(List.of("Chips rally (CNBC)"));
        when(bull.propose(anyList())).thenReturn(List.of(nvda));
        when(judge.judge(anyList())).thenReturn(new StockJudgeAgent.Verdict(List.of(nvda), List.of()));
        when(quoteService.snapshot(anyList())).thenReturn(Map.of("NVDA", 120.0));
        doAnswer(inv -> {
            RunResult r = inv.getArgument(0);
            r.plannedShares.put("NVDA", 2);
            return null;
        }).when(capital).allocate(any(), anyList(), any());
    }

    // Positive: full execute run — fills, totals, trace order, persisted.
    @Test
    void executeRunCompletesWithFillsAndTrace() {
        stubHappyPath();
        RunResult result = supervisor(60000).run(true, "demo");

        assertEquals("completed", result.status);
        assertEquals(240.0, result.totalInvested, 0.001);
        assertEquals(760.0, result.remainingAllowance, 0.001);
        assertEquals(2.0, result.executedTrades.get("NVDA").get("shares"), 0.001);

        List<String> agents = result.agentTrace.stream().map(s -> s.agent).toList();
        assertEquals(List.of("news_scout", "bull", "bear", "stock_judge", "quote_snapshot", "executor"), agents);

        ArgumentCaptor<AgentRun> saved = ArgumentCaptor.forClass(AgentRun.class);
        verify(runRepository).save(saved.capture());
        assertEquals("completed", saved.getValue().getStatus());
        assertEquals("demo", saved.getValue().getUsername());
    }

    // Negative: dependency failure propagates as 503-style exception, run stored as failed.
    @Test
    void dependencyFailurePersistsFailedRunAndRethrows() {
        when(newsScout.fetchHeadlines())
                .thenThrow(new AgentUnavailableException("FINNHUB_API_KEY is not configured on the server"));

        assertThrows(AgentUnavailableException.class, () -> supervisor(60000).run(true, null));

        ArgumentCaptor<AgentRun> saved = ArgumentCaptor.forClass(AgentRun.class);
        verify(runRepository).save(saved.capture());
        assertEquals("failed", saved.getValue().getStatus());
        assertTrue(saved.getValue().getError().contains("FINNHUB_API_KEY"));
    }

    // Negative: nothing advances past the judge → clean stop, no capital or fills.
    @Test
    void stopsCleanlyWhenJudgeRejectsEverything() {
        Candidate weak = new Candidate("XYZ", "meh", 0.3);
        when(newsScout.fetchHeadlines()).thenReturn(List.of("h"));
        when(bull.propose(anyList())).thenReturn(List.of(weak));
        when(judge.judge(anyList())).thenReturn(new StockJudgeAgent.Verdict(
                List.of(), List.of(Map.of("ticker", "XYZ", "reason", "weak"))));

        RunResult result = supervisor(60000).run(true, null);
        assertEquals("completed", result.status);
        assertTrue(result.executedTrades.isEmpty());
        verify(capital, never()).allocate(any(), anyList(), any());
        assertTrue(result.agentTrace.stream()
                .anyMatch(s -> "supervisor".equals(s.agent) && "stopped".equals(s.status)));
    }

    // Negative: exhausted wall-clock budget → partial status with timeout trace.
    @Test
    void timeoutProducesPartialResult() {
        when(newsScout.fetchHeadlines()).thenReturn(List.of("h"));

        RunResult result = supervisor(-1).run(true, null);
        assertEquals("partial", result.status);
        assertTrue(result.error.contains("wall-clock"));
        TraceStep last = result.agentTrace.get(result.agentTrace.size() - 1);
        assertEquals("timeout", last.status);
        verify(bull, never()).propose(anyList());
    }
}
