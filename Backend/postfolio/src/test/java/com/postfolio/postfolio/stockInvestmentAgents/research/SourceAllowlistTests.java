package com.postfolio.postfolio.stockInvestmentAgents.research;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceAllowlistTests {

    private final SourceAllowlist allowlist = new SourceAllowlist();

    @Test
    void acceptsAllowlistedHttpsRss() {
        assertTrue(allowlist.isAllowed("https://feeds.bbci.co.uk/news/business/rss.xml"));
    }

    @Test
    void rejectsUnknownHost() {
        assertFalse(allowlist.isAllowed("https://evil.example.com/news"));
    }

    @Test
    void rejectsBlankAndNonHttp() {
        assertFalse(allowlist.isAllowed(""));
        assertFalse(allowlist.isAllowed(null));
        assertFalse(allowlist.isAllowed("ftp://feeds.bbci.co.uk/x"));
    }

    @Test
    void rejectsMalformedUrl() {
        assertFalse(allowlist.isAllowed("not a url"));
    }
}
