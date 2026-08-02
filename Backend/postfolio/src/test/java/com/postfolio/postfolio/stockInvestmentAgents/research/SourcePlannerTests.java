package com.postfolio.postfolio.stockInvestmentAgents.research;

import com.postfolio.postfolio.stockInvestmentAgents.debate.GroqClientTestSupport;
import com.postfolio.postfolio.stockInvestmentAgents.groq.GroqClient;
import com.postfolio.postfolio.stockInvestmentAgents.groq.GroqConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SourcePlannerTests {

    private static final GroqConfig WITH_KEY =
            new GroqConfig("key", "fast", "judge", 1000, 0.15, 60000);
    private static final GroqConfig NO_KEY =
            new GroqConfig("", "fast", "judge", 1000, 0.15, 60000);

    // Positive: Groq returns allowlisted URLs that become the plan.
    @Test
    void usesAllowlistedUrlsFromGroq() {
        GroqClient groq = mock(GroqClient.class);
        when(groq.chatJson(anyString(), anyString(), anyString()))
                .thenReturn(GroqClientTestSupport.parse("""
                        {"targets":[{"url":"https://feeds.bbci.co.uk/news/business/rss.xml","why":"news"}]}
                        """));
        List<String> plan = new SourcePlanner(groq, WITH_KEY, new SourceAllowlist()).plan(5);
        assertEquals(1, plan.size());
        assertTrue(plan.get(0).contains("bbci.co.uk"));
    }

    // Negative: blank Groq key falls back to default seeds.
    @Test
    void blankKeyFallsBackToDefaults() {
        List<String> plan = new SourcePlanner(mock(GroqClient.class), NO_KEY, new SourceAllowlist()).plan(3);
        assertFalse(plan.isEmpty());
        assertTrue(plan.size() <= 3);
    }

    // Edge: non-allowlisted Groq URLs are dropped; empty → defaults.
    @Test
    void rejectsNonAllowlistedGroqUrls() {
        GroqClient groq = mock(GroqClient.class);
        when(groq.chatJson(anyString(), anyString(), anyString()))
                .thenReturn(GroqClientTestSupport.parse("""
                        {"targets":[{"url":"https://evil.example/rss","why":"nope"}]}
                        """));
        List<String> plan = new SourcePlanner(groq, WITH_KEY, new SourceAllowlist()).plan(4);
        assertFalse(plan.isEmpty());
        assertTrue(plan.stream().noneMatch(u -> u.contains("evil.example")));
    }

    // Failure: Groq throw → soft fallback to defaults.
    @Test
    void groqFailureFallsBackToDefaults() {
        GroqClient groq = mock(GroqClient.class);
        when(groq.chatJson(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("down"));
        List<String> plan = new SourcePlanner(groq, WITH_KEY, new SourceAllowlist()).plan(2);
        assertEquals(2, plan.size());
    }
}
