package com.postfolio.postfolio.stockInvestmentAgents.capital;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Hard book rules with one deterministic fix round (docs/agent-trader-v2.md §6.9):
 * 1. valid tickers  2. <= max positions  3. single-name weight <= 35% of starting
 * allowance  4. total cost <= starting allowance - reserve  5. no duplicates
 * (impossible by Map, kept for the record). Violations are fixed by trimming
 * shares / dropping the smallest lines; anything cut goes back to cash.
 */
@Service
public class RiskBookGate {

    static final int MAX_POSITIONS = 5;
    static final double MAX_SINGLE_NAME_WEIGHT = 0.35;
    static final Pattern TICKER = Pattern.compile("^[A-Z]{1,5}$");

    public record GateResult(Map<String, Integer> approvedShares,
                             List<String> violations,
                             List<Map<String, Object>> adjustments) {}

    public GateResult check(Map<String, Integer> proposedShares, Map<String, Double> quotes,
                            double startingAllowance, double reserveTarget) {
        double maxSpend = startingAllowance - reserveTarget;
        double maxPerName = startingAllowance * MAX_SINGLE_NAME_WEIGHT;
        List<String> violations = new ArrayList<>();
        List<Map<String, Object>> adjustments = new ArrayList<>();
        Map<String, Integer> book = new LinkedHashMap<>();

        // Rule 1: valid, quoted tickers only.
        for (Map.Entry<String, Integer> entry : proposedShares.entrySet()) {
            String ticker = entry.getKey();
            if (!TICKER.matcher(ticker).matches() || quotes.get(ticker) == null || quotes.get(ticker) <= 0) {
                violations.add("invalid_ticker:" + ticker);
                adjustments.add(Map.of("ticker", ticker, "action", "dropped", "reason", "invalid_ticker"));
            } else if (entry.getValue() > 0) {
                book.put(ticker, entry.getValue());
            }
        }

        // Rule 2: max positions — keep the largest notionals.
        if (book.size() > MAX_POSITIONS) {
            violations.add("max_positions_exceeded:" + book.size());
            List<String> byNotional = book.keySet().stream()
                    .sorted((a, b) -> Double.compare(book.get(b) * quotes.get(b), book.get(a) * quotes.get(a)))
                    .toList();
            for (String ticker : byNotional.subList(MAX_POSITIONS, byNotional.size())) {
                book.remove(ticker);
                adjustments.add(Map.of("ticker", ticker, "action", "dropped", "reason", "max_positions"));
            }
        }

        // Rule 3: single-name weight cap (of starting allowance) — trim shares.
        for (Map.Entry<String, Integer> entry : new LinkedHashMap<>(book).entrySet()) {
            String ticker = entry.getKey();
            double price = quotes.get(ticker);
            if (entry.getValue() * price > maxPerName) {
                violations.add("single_name_weight:" + ticker);
                int trimmed = (int) Math.floor(maxPerName / price);
                if (trimmed <= 0) {
                    book.remove(ticker);
                    adjustments.add(Map.of("ticker", ticker, "action", "dropped", "reason", "single_name_weight"));
                } else {
                    book.put(ticker, trimmed);
                    adjustments.add(Map.of("ticker", ticker, "action", "trimmed",
                            "reason", "single_name_weight", "shares", trimmed));
                }
            }
        }

        // Rule 4: never breach the cash reserve floor — trim smallest lines first.
        double totalCost = cost(book, quotes);
        if (totalCost > maxSpend) {
            violations.add("cash_floor_breached");
            List<String> bySmallest = book.keySet().stream()
                    .sorted((a, b) -> Double.compare(book.get(a) * quotes.get(a), book.get(b) * quotes.get(b)))
                    .toList();
            for (String ticker : bySmallest) {
                while (book.containsKey(ticker) && cost(book, quotes) > maxSpend) {
                    int shares = book.get(ticker) - 1;
                    if (shares <= 0) {
                        book.remove(ticker);
                        adjustments.add(Map.of("ticker", ticker, "action", "dropped", "reason", "cash_floor"));
                    } else {
                        book.put(ticker, shares);
                    }
                }
                if (cost(book, quotes) <= maxSpend) {
                    if (book.containsKey(ticker)) {
                        adjustments.add(Map.of("ticker", ticker, "action", "trimmed",
                                "reason", "cash_floor", "shares", book.get(ticker)));
                    }
                    break;
                }
            }
        }

        return new GateResult(book, violations, adjustments);
    }

    private static double cost(Map<String, Integer> book, Map<String, Double> quotes) {
        return book.entrySet().stream()
                .mapToDouble(e -> e.getValue() * quotes.get(e.getKey()))
                .sum();
    }
}
