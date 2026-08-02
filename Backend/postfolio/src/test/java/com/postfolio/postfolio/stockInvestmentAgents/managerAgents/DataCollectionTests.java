package com.postfolio.postfolio.stockInvestmentAgents.managerAgents;

import com.postfolio.postfolio.stockInvestmentAgents.AgentUnavailableException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataCollectionTests {

    @Test
    void chunkSplitsHeadlinesIntoGroupsOfRequestedSize() {
        List<String> items = List.of("a", "b", "c", "d", "e");
        List<List<String>> grouped = dataCollection.chunk(items, 2);
        assertEquals(3, grouped.size());
        assertEquals(List.of("a", "b"), grouped.get(0));
        assertEquals(List.of("e"), grouped.get(2));
    }

    @Test
    void chunkHandlesEmptyList() {
        assertTrue(dataCollection.chunk(List.of(), 25).isEmpty());
    }

    @Test
    void missingApiKeyFailsFastWithClearMessage() {
        dataCollection collector = new dataCollection("");
        AgentUnavailableException error =
                assertThrows(AgentUnavailableException.class, collector::fetchGeneralMarketNews);
        assertTrue(error.getMessage().contains("FINNHUB_API_KEY"));
    }

    @Test
    void nullApiKeyFailsFastWithClearMessage() {
        dataCollection collector = new dataCollection(null);
        assertThrows(AgentUnavailableException.class, collector::fetchGeneralMarketNews);
    }
}
