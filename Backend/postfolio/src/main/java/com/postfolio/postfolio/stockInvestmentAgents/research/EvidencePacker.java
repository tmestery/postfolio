package com.postfolio.postfolio.stockInvestmentAgents.research;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Dedupes and ranks evidence for debate (docs/agent-trader-v3.md §5.3). */
@Service
public class EvidencePacker {

    public static final int MAX_ITEMS = 40;

    public List<EvidenceItem> pack(List<EvidenceItem> raw) {
        if (raw == null || raw.isEmpty()) return List.of();
        LinkedHashMap<String, EvidenceItem> byKey = new LinkedHashMap<>();
        for (EvidenceItem item : raw) {
            if (item == null || item.title == null || item.title.isBlank()) continue;
            String key = normalizeKey(item.title);
            byKey.putIfAbsent(key, item);
            if (byKey.size() >= MAX_ITEMS) break;
        }
        List<EvidenceItem> ranked = new ArrayList<>(byKey.values());
        ranked.sort((a, b) -> Integer.compare(score(b), score(a)));
        if (ranked.size() > MAX_ITEMS) return ranked.subList(0, MAX_ITEMS);
        return ranked;
    }

    public List<String> headlines(List<EvidenceItem> packed) {
        return packed.stream().map(EvidenceItem::asHeadline).toList();
    }

    public List<Map<String, Object>> toDto(List<EvidenceItem> packed) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (EvidenceItem item : packed) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("title", item.title);
            row.put("url", item.sourceUrl);
            row.put("bullets", item.bullets == null ? List.of() : item.bullets);
            row.put("tickers", item.tickers == null ? List.of() : item.tickers);
            out.add(row);
        }
        return out;
    }

    private static int score(EvidenceItem item) {
        int s = 0;
        if (item.tickers != null) s += item.tickers.size() * 3;
        if (item.bullets != null && !item.bullets.isEmpty()) s += 1;
        if (item.sourceUrl != null && item.sourceUrl.contains("rss")) s += 1;
        return s;
    }

    private static String normalizeKey(String title) {
        return title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").strip();
    }
}
