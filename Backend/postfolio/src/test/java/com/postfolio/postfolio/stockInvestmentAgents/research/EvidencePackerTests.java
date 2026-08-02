package com.postfolio.postfolio.stockInvestmentAgents.research;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvidencePackerTests {

    private final EvidencePacker packer = new EvidencePacker();

    @Test
    void packsAndDedupesByTitle() {
        EvidenceItem a = new EvidenceItem("NVDA rallies", "https://a");
        a.tickers.add("NVDA");
        EvidenceItem dup = new EvidenceItem("nvda rallies", "https://b");
        EvidenceItem b = new EvidenceItem("Oil slips", "https://c");
        List<EvidenceItem> packed = packer.pack(List.of(a, dup, b));
        assertEquals(2, packed.size());
        assertTrue(packer.headlines(packed).get(0).contains("NVDA"));
    }

    @Test
    void emptyInputReturnsEmpty() {
        assertTrue(packer.pack(List.of()).isEmpty());
        assertTrue(packer.pack(null).isEmpty());
    }

    @Test
    void skipsBlankTitles() {
        assertTrue(packer.pack(List.of(new EvidenceItem("  ", "https://x"))).isEmpty());
    }

    @Test
    void toDtoIncludesUrl() {
        EvidenceItem a = new EvidenceItem("Hello", "https://feeds.bbci.co.uk/x");
        assertEquals("https://feeds.bbci.co.uk/x", packer.toDto(List.of(a)).get(0).get("url"));
    }
}
