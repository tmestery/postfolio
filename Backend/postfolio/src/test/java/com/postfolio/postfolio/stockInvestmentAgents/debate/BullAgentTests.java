package com.postfolio.postfolio.stockInvestmentAgents.debate;

import com.postfolio.postfolio.stockInvestmentAgents.groq.GroqClient;
import com.postfolio.postfolio.stockInvestmentAgents.groq.GroqConfig;
import com.postfolio.postfolio.stockInvestmentAgents.model.Candidate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BullAgentTests {

    private static final GroqConfig CONFIG =
            new GroqConfig("key", "fast", "judge", 1000, 0.15, 60000);

    private BullAgent agentReturning(String content) {
        GroqClient groq = mock(GroqClient.class);
        when(groq.chatJson(anyString(), anyString(), anyString()))
                .thenAnswer(inv -> GroqClientTestSupport.parse(content));
        return new BullAgent(groq, CONFIG);
    }

    // Positive: well-formed model output becomes validated candidates.
    @Test
    void parsesCandidatesFromModelOutput() {
        List<Candidate> result = agentReturning("""
                {"candidates":[{"ticker":"NVDA","thesis":"AI demand","confidence":0.82},
                               {"ticker":"aapl","thesis":"lowercase ok","confidence":0.6}]}
                """).propose(List.of("headline"));
        assertEquals(2, result.size());
        assertEquals("NVDA", result.get(0).ticker);
        assertEquals("AAPL", result.get(1).ticker); // normalized to uppercase
    }

    // Negative: invalid ticker symbols are stripped.
    @Test
    void stripsInvalidTickers() {
        List<Candidate> result = agentReturning("""
                {"candidates":[{"ticker":"TOOLONGG","thesis":"x","confidence":0.5},
                               {"ticker":"BRK.B","thesis":"x","confidence":0.5},
                               {"ticker":"MSFT","thesis":"x","confidence":0.5}]}
                """).propose(List.of("headline"));
        assertEquals(1, result.size());
        assertEquals("MSFT", result.get(0).ticker);
    }

    // Negative: duplicate tickers collapse to one candidate.
    @Test
    void deduplicatesTickers() {
        List<Candidate> result = agentReturning("""
                {"candidates":[{"ticker":"NVDA","thesis":"a","confidence":0.9},
                               {"ticker":"NVDA","thesis":"b","confidence":0.8}]}
                """).propose(List.of("headline"));
        assertEquals(1, result.size());
    }

    // Negative: candidate list capped and confidences clamped to [0,1].
    @Test
    void capsCandidatesAndClampsConfidence() {
        StringBuilder json = new StringBuilder("{\"candidates\":[");
        for (int i = 0; i < 12; i++) {
            if (i > 0) json.append(',');
            json.append("{\"ticker\":\"T").append((char) ('A' + i))
                    .append("\",\"thesis\":\"x\",\"confidence\":5}");
        }
        json.append("]}");
        List<Candidate> result = agentReturning(json.toString()).propose(List.of("headline"));
        assertEquals(BullAgent.MAX_CANDIDATES, result.size());
        assertTrue(result.stream().allMatch(c -> c.confidence <= 1.0));
    }
}
