package com.postfolio.postfolio.models.symbol;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class StockSymbolServiceTests {

    @Autowired
    private StockSymbolService stockSymbolService;

    @Test
    void isKnownAcceptsSeededTickerCaseInsensitive() {
        assertTrue(stockSymbolService.isKnown("aapl"));
        assertTrue(stockSymbolService.isKnown(" AAPL "));
    }

    @Test
    void isKnownRejectsUnknownTicker() {
        assertFalse(stockSymbolService.isKnown("NOTAREALTICKER"));
    }

    @Test
    void isKnownRejectsBlank() {
        assertFalse(stockSymbolService.isKnown(""));
        assertFalse(stockSymbolService.isKnown("   "));
        assertFalse(stockSymbolService.isKnown(null));
    }

    @Test
    void searchCapsResultsAndOrdersBySymbol() {
        List<StockSymbol> matches = stockSymbolService.search("A", 3);
        assertEquals(3, matches.size());
        assertTrue(matches.get(0).getSymbol().compareTo(matches.get(1).getSymbol()) <= 0);
    }
}
