package com.postfolio.postfolio.stockInvestmentAgents.portfolio;

import com.postfolio.postfolio.models.portfolio.AgentPortfolio;
import com.postfolio.postfolio.models.portfolio.AgentPortfolioMarkRepository;
import com.postfolio.postfolio.models.portfolio.AgentPortfolioRepository;
import com.postfolio.postfolio.models.portfolio.AgentPosition;
import com.postfolio.postfolio.models.portfolio.AgentPositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTests {

    private static final double SEED = 1000.0;

    @Mock private AgentPortfolioRepository portfolioRepository;
    @Mock private AgentPositionRepository positionRepository;
    @Mock private AgentPortfolioMarkRepository markRepository;

    private PortfolioService service;

    @BeforeEach
    void setUp() {
        service = new PortfolioService(portfolioRepository, positionRepository, markRepository, SEED);
        AgentPortfolio book = new AgentPortfolio();
        book.setStartingCash(SEED);
        book.setCash(SEED);
        when(portfolioRepository.findById(AgentPortfolio.SINGLETON_ID)).thenReturn(Optional.of(book));
        when(portfolioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(positionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void stubPositions(AgentPosition... positions) {
        when(positionRepository.findAll()).thenReturn(java.util.List.of(positions));
    }

    // Positive: apply fill reduces cash and opens a position.
    @Test
    void applyFillReducesCashAndOpensPosition() {
        Map<String, Map<String, Double>> fills = Map.of(
                "NVDA", Map.of("shares", 2.0, "price", 100.0, "cost", 200.0));
        when(positionRepository.findById("NVDA")).thenReturn(Optional.empty());
        when(positionRepository.findAll()).thenAnswer(inv -> {
            AgentPosition row = new AgentPosition();
            row.setTicker("NVDA");
            row.setShares(2);
            row.setAvgCost(100);
            row.setCostBasis(200);
            row.setMarkPrice(100.0);
            return java.util.List.of(row);
        });

        PortfolioSnapshot snap = service.applyFills(fills);

        assertEquals(800.0, snap.cash, 0.001);
        assertEquals(1, snap.positions.size());
        assertEquals("NVDA", snap.positions.get(0).ticker);
        verify(markRepository).save(any());
    }

    // Negative: fill larger than cash is skipped.
    @Test
    void rejectsFillOverCash() {
        Map<String, Map<String, Double>> fills = Map.of(
                "AAPL", Map.of("shares", 20.0, "price", 100.0, "cost", 2000.0));

        PortfolioSnapshot snap = service.applyFills(fills);

        assertEquals(SEED, snap.cash, 0.001);
        verify(positionRepository, never()).save(any());
    }

    // Edge: second buy averages cost.
    @Test
    void secondBuyAveragesCost() {
        AgentPosition existing = new AgentPosition();
        existing.setTicker("MSFT");
        existing.setShares(2);
        existing.setAvgCost(100);
        existing.setCostBasis(200);
        when(positionRepository.findById("MSFT")).thenReturn(Optional.of(existing));
        stubPositions(existing);

        Map<String, Map<String, Double>> fills = Map.of(
                "MSFT", Map.of("shares", 2.0, "price", 120.0, "cost", 240.0));
        service.applyFills(fills);

        ArgumentCaptor<AgentPosition> captor = ArgumentCaptor.forClass(AgentPosition.class);
        verify(positionRepository).save(captor.capture());
        assertEquals(4.0, captor.getValue().getShares(), 0.001);
        assertEquals(110.0, captor.getValue().getAvgCost(), 0.001);
    }

    // Failure: reset restores seed and clears positions.
    @Test
    void resetRestoresSeed() {
        stubPositions();
        PortfolioSnapshot snap = service.reset();
        verify(positionRepository).deleteAll();
        verify(markRepository).deleteAll();
        assertEquals(SEED, snap.cash, 0.001);
        assertEquals(SEED, snap.startingCashSeed, 0.001);
        assertTrue(snap.positions.isEmpty());
    }
}
