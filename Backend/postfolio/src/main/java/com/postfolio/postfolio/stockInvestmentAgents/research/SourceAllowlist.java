package com.postfolio.postfolio.stockInvestmentAgents.research;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Host allowlist for public research fetches (docs/agent-trader-v3.md §7). */
@Component
public class SourceAllowlist {

    /** Default seed targets used when the planner is unavailable. */
    public static final List<String> DEFAULT_TARGETS = List.of(
            "https://feeds.bbci.co.uk/news/business/rss.xml",
            "https://finance.yahoo.com/news/rssindex",
            "https://www.cnbc.com/id/100003114/device/rss/rss.html"
    );

    private static final Set<String> ALLOWED_HOSTS = Set.of(
            "feeds.bbci.co.uk",
            "www.bbc.com",
            "bbc.com",
            "finance.yahoo.com",
            "www.cnbc.com",
            "cnbc.com",
            "www.reuters.com",
            "reuters.com",
            "www.marketwatch.com",
            "marketwatch.com",
            "query1.finance.yahoo.com"
    );

    public boolean isAllowed(String url) {
        if (url == null || url.isBlank()) return false;
        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                return false;
            }
            String host = uri.getHost();
            if (host == null) return false;
            host = host.toLowerCase(Locale.ROOT);
            if (ALLOWED_HOSTS.contains(host)) return true;
            // Allow subdomains of yahoo finance quote hosts.
            return host.endsWith(".yahoo.com") && host.contains("finance");
        } catch (Exception e) {
            return false;
        }
    }

    public List<String> defaultTargets() {
        return DEFAULT_TARGETS;
    }
}
