package com.postfolio.postfolio.stockInvestmentAgents.research;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebScoutTests {

    @Test
    void fetchesAllowlistedUrlAndExtractsItems() {
        PageFetcher fetcher = url -> Optional.of("""
                <?xml version="1.0"?><rss><channel>
                <item><title>Tech leads</title><link>https://finance.yahoo.com/a</link></item>
                </channel></rss>
                """);
        WebScout scout = new WebScout(fetcher, new HtmlTextExtractor(), new SourceAllowlist());
        WebScout.ScoutReport report = scout.scout(List.of("https://finance.yahoo.com/news/rssindex"));
        assertEquals(1, report.fetchedOk());
        assertEquals(1, report.items().size());
        assertEquals("Tech leads", report.items().get(0).title);
    }

    @Test
    void skipsNonAllowlistedUrl() {
        WebScout scout = new WebScout(url -> Optional.of("<rss/>"), new HtmlTextExtractor(), new SourceAllowlist());
        WebScout.ScoutReport report = scout.scout(List.of("https://evil.example/x"));
        assertEquals(0, report.fetchedOk());
        assertTrue(report.skipped().get(0).contains("not allowlisted"));
    }

    @Test
    void emptyFetchIsSoftSkipped() {
        WebScout scout = new WebScout(url -> Optional.empty(), new HtmlTextExtractor(), new SourceAllowlist());
        WebScout.ScoutReport report = scout.scout(List.of("https://feeds.bbci.co.uk/news/business/rss.xml"));
        assertEquals(0, report.fetchedOk());
        assertTrue(report.items().isEmpty());
    }

    @Test
    void nullUrlListIsSafe() {
        WebScout scout = new WebScout(url -> Optional.empty(), new HtmlTextExtractor(), new SourceAllowlist());
        assertEquals(0, scout.scout(null).fetchedOk());
    }
}
