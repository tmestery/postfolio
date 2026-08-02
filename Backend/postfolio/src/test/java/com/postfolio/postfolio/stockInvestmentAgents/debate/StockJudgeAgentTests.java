package com.postfolio.postfolio.stockInvestmentAgents.debate;

import com.postfolio.postfolio.stockInvestmentAgents.groq.GroqClient;
import com.postfolio.postfolio.stockInvestmentAgents.groq.GroqConfig;
import com.postfolio.postfolio.stockInvestmentAgents.model.Candidate;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockJudgeAgentTests {

    private static final GroqConfig CONFIG =
            new GroqConfig("key", "fast", "judge", 1000, 0.15, 60000);

    private static List<Candidate> candidates(String... tickers) {
        List<Candidate> list = new ArrayList<>();
        for (String t : tickers) list.add(new Candidate(t, "thesis " + t, 0.7));
        return list;
    }

    private StockJudgeAgent agentReturning(String content) {
        GroqClient groq = mock(GroqClient.class);
        when(groq.chatJson(anyString(), anyString(), anyString()))
                .thenAnswer(inv -> GroqClientTestSupport.parse(content));
        return new StockJudgeAgent(groq, CONFIG);
    }

    // Positive: advanced tickers come back scored and sorted by score.
    @Test
    void advancesAndRanksCandidates() {
        StockJudgeAgent.Verdict verdict = agentReturning("""
                {"ranked":[{"ticker":"AAPL","score":6.5,"decision":"advance","rationale":"solid"},
                           {"ticker":"NVDA","score":8.1,"decision":"advance","rationale":"strong"}],
                 "rejected":[]}
                """).judge(candidates("NVDA", "AAPL"));
        assertEquals(2, verdict.advanced().size());
        assertEquals("NVDA", verdict.advanced().get(0).ticker); // higher score first
    }

    // Negative: judge can reject everything → empty shortlist, reasons captured.
    @Test
    void allRejectedYieldsEmptyShortlist() {
        StockJudgeAgent.Verdict verdict = agentReturning("""
                {"ranked":[{"ticker":"NVDA","score":2.0,"decision":"reject","rationale":"hype"}],
                 "rejected":[{"ticker":"AAPL","reason":"no support"}]}
                """).judge(candidates("NVDA", "AAPL"));
        assertTrue(verdict.advanced().isEmpty());
        assertEquals(2, verdict.rejected().size());
    }

    // Negative: bear-rejected candidates never reach the judge model.
    @Test
    void bearRejectedCandidatesSkipTheJudge() {
        GroqClient groq = mock(GroqClient.class);
        StockJudgeAgent judge = new StockJudgeAgent(groq, CONFIG);
        List<Candidate> input = candidates("NVDA");
        input.get(0).decision = "reject";
        input.get(0).rationale = "Bear agent flagged as uninvestable";

        StockJudgeAgent.Verdict verdict = judge.judge(input);
        assertTrue(verdict.advanced().isEmpty());
        assertEquals(1, verdict.rejected().size());
        verify(groq, never()).chatJson(anyString(), anyString(), anyString());
    }

    // Negative: tickers invented by the judge (not in the debate) are ignored.
    @Test
    void ignoresTickersInventedByTheModel() {
        StockJudgeAgent.Verdict verdict = agentReturning("""
                {"ranked":[{"ticker":"HALL","score":9.9,"decision":"advance","rationale":"hallucinated"},
                           {"ticker":"NVDA","score":7.0,"decision":"advance","rationale":"real"}],
                 "rejected":[]}
                """).judge(candidates("NVDA"));
        assertEquals(1, verdict.advanced().size());
        assertEquals("NVDA", verdict.advanced().get(0).ticker);
    }
}
