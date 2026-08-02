package com.postfolio.postfolio.stockInvestmentAgents.research;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** HTML/RSS → titles + plain text (docs/agent-trader-v3.md §5.2). */
@Component
public class HtmlTextExtractor {

    private static final Pattern TICKER = Pattern.compile("\\b([A-Z]{1,5})\\b");
    private static final Set<String> STOP = Set.of(
            "A", "I", "CEO", "US", "USA", "EU", "UK", "GDP", "AI", "IPO", "ETF", "THE", "AND", "FOR", "WITH"
    );

    public List<EvidenceItem> extract(String url, String body) {
        if (body == null || body.isBlank()) return List.of();
        String trimmed = body.stripLeading();
        if (looksLikeRss(trimmed)) {
            return fromRss(url, trimmed);
        }
        return fromHtml(url, trimmed);
    }

    static boolean looksLikeRss(String body) {
        String head = body.substring(0, Math.min(400, body.length())).toLowerCase(Locale.ROOT);
        return head.contains("<rss") || head.contains("<feed") || head.contains("<rdf:rdf");
    }

    List<EvidenceItem> fromRss(String url, String body) {
        Document doc = Jsoup.parse(body, url, Parser.xmlParser());
        Elements items = doc.select("item");
        if (items.isEmpty()) items = doc.select("entry");
        List<EvidenceItem> out = new ArrayList<>();
        for (Element item : items) {
            if (out.size() >= 25) break;
            String title = text(item.selectFirst("title"));
            if (title.isBlank()) continue;
            String link = text(item.selectFirst("link"));
            if (link.isBlank()) {
                Element linkEl = item.selectFirst("link[href]");
                if (linkEl != null) link = linkEl.attr("href");
            }
            if (link.isBlank()) link = url;
            EvidenceItem evidence = new EvidenceItem(title, link);
            evidence.tickers.addAll(guessTickers(title));
            String desc = text(item.selectFirst("description"));
            if (desc.isBlank()) desc = text(item.selectFirst("summary"));
            if (!desc.isBlank()) evidence.bullets.add(truncate(desc, 240));
            out.add(evidence);
        }
        return out;
    }

    List<EvidenceItem> fromHtml(String url, String body) {
        Document doc = Jsoup.parse(body, url);
        doc.select("script, style, nav, footer, noscript").remove();
        String title = doc.title();
        if (title == null || title.isBlank()) {
            Element h1 = doc.selectFirst("h1");
            title = h1 == null ? "Untitled" : h1.text();
        }
        EvidenceItem item = new EvidenceItem(title.trim(), url);
        String text = doc.body() == null ? "" : doc.body().text();
        item.bullets.add(truncate(text, 400));
        item.tickers.addAll(guessTickers(title + " " + text.substring(0, Math.min(500, text.length()))));
        return List.of(item);
    }

    static List<String> guessTickers(String text) {
        if (text == null) return List.of();
        LinkedHashSet<String> found = new LinkedHashSet<>();
        Matcher m = TICKER.matcher(text);
        while (m.find()) {
            String t = m.group(1);
            if (!STOP.contains(t)) found.add(t);
            if (found.size() >= 8) break;
        }
        return new ArrayList<>(found);
    }

    private static String text(Element el) {
        return el == null ? "" : el.text().trim();
    }

    private static String truncate(String s, int max) {
        String t = s.replaceAll("\\s+", " ").trim();
        return t.length() <= max ? t : t.substring(0, max - 1) + "…";
    }
}
