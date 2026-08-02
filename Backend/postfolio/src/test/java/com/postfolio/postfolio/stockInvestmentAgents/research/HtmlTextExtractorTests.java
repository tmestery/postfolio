package com.postfolio.postfolio.stockInvestmentAgents.research;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlTextExtractorTests {

    private final HtmlTextExtractor extractor = new HtmlTextExtractor();

    @Test
    void extractsTitlesFromRss() {
        String rss = """
                <?xml version="1.0"?>
                <rss><channel>
                  <item><title>NVDA jumps on AI demand</title><link>https://example.com/a</link>
                  <description>Chip stocks rally</description></item>
                  <item><title>Markets steady</title><link>https://example.com/b</link></item>
                </channel></rss>
                """;
        List<EvidenceItem> items = extractor.extract("https://feeds.bbci.co.uk/news/business/rss.xml", rss);
        assertEquals(2, items.size());
        assertEquals("NVDA jumps on AI demand", items.get(0).title);
        assertTrue(items.get(0).tickers.contains("NVDA"));
    }

    @Test
    void emptyBodyReturnsEmptyList() {
        assertTrue(extractor.extract("https://feeds.bbci.co.uk/x", "   ").isEmpty());
    }

    @Test
    void htmlPageYieldsSingleItem() {
        String html = "<html><head><title>Apple rises</title></head><body><p>AAPL gains</p></body></html>";
        List<EvidenceItem> items = extractor.extract("https://www.cnbc.com/story", html);
        assertEquals(1, items.size());
        assertEquals("Apple rises", items.get(0).title);
    }

    @Test
    void looksLikeRssDetectsFeed() {
        assertTrue(HtmlTextExtractor.looksLikeRss("<rss version=\"2.0\">"));
        assertFalse(HtmlTextExtractor.looksLikeRss("<html><body>hi</body></html>"));
    }
}
