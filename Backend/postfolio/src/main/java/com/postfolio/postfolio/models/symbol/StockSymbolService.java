package com.postfolio.postfolio.models.symbol;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class StockSymbolService {

    private final StockSymbolRepository repository;

    public StockSymbolService(StockSymbolRepository repository) {
        this.repository = repository;
    }

    /** Normalize to uppercase ticker form used as the table PK. */
    public String normalize(String raw) {
        if (raw == null) return null;
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    public boolean isKnown(String raw) {
        String symbol = normalize(raw);
        if (symbol == null || symbol.isEmpty()) return false;
        return repository.existsBySymbol(symbol);
    }

    /**
     * Prefix search for the create-post typeahead.
     * Empty/blank query returns an empty list (callers should not dump the whole book).
     */
    public List<StockSymbol> search(String query, int limit) {
        String prefix = normalize(query);
        if (prefix == null || prefix.isEmpty()) return List.of();
        int capped = Math.max(1, Math.min(limit, 50));
        List<StockSymbol> matches = repository.findBySymbolStartingWithOrderBySymbolAsc(prefix);
        if (matches.size() <= capped) return matches;
        return matches.subList(0, capped);
    }
}
