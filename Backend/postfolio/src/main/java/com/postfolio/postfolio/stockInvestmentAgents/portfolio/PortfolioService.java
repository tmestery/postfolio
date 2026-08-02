package com.postfolio.postfolio.stockInvestmentAgents.portfolio;

import com.postfolio.postfolio.models.portfolio.AgentPortfolio;
import com.postfolio.postfolio.models.portfolio.AgentPortfolioMark;
import com.postfolio.postfolio.models.portfolio.AgentPortfolioMarkRepository;
import com.postfolio.postfolio.models.portfolio.AgentPortfolioRepository;
import com.postfolio.postfolio.models.portfolio.AgentPosition;
import com.postfolio.postfolio.models.portfolio.AgentPositionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * House paper portfolio — simulated fills only (docs/agent-trader-v4.md).
 * No real money; Alpaca is used later for marks only.
 */
@Service
public class PortfolioService {

    private final AgentPortfolioRepository portfolioRepository;
    private final AgentPositionRepository positionRepository;
    private final AgentPortfolioMarkRepository markRepository;
    private final double startingCashSeed;

    public PortfolioService(AgentPortfolioRepository portfolioRepository,
                            AgentPositionRepository positionRepository,
                            AgentPortfolioMarkRepository markRepository,
                            @Value("${AGENT_STARTING_CASH:${AGENT_ALLOWANCE:1000}}") double startingCashSeed) {
        this.portfolioRepository = portfolioRepository;
        this.positionRepository = positionRepository;
        this.markRepository = markRepository;
        this.startingCashSeed = startingCashSeed;
    }

    public double getStartingCashSeed() {
        return startingCashSeed;
    }

    @Transactional
    public AgentPortfolio getOrCreate() {
        Optional<AgentPortfolio> existing = portfolioRepository.findById(AgentPortfolio.SINGLETON_ID);
        if (existing.isPresent()) {
            return existing.get();
        }
        AgentPortfolio created = new AgentPortfolio();
        created.setStartingCash(startingCashSeed);
        created.setCash(startingCashSeed);
        created.setUpdatedAt(Instant.now());
        return portfolioRepository.save(created);
    }

    /** Apply simulated execute fills; skips any line that would overdraw cash. */
    @Transactional
    public PortfolioSnapshot applyFills(Map<String, Map<String, Double>> executedTrades) {
        if (executedTrades == null || executedTrades.isEmpty()) {
            return snapshot();
        }
        AgentPortfolio book = getOrCreate();
        for (Map.Entry<String, Map<String, Double>> entry : executedTrades.entrySet()) {
            String ticker = entry.getKey();
            Map<String, Double> fill = entry.getValue();
            if (fill == null) continue;
            double shares = fill.getOrDefault("shares", 0.0);
            double price = fill.getOrDefault("price", 0.0);
            if (shares <= 0 || price <= 0) continue;
            double cost = shares * price;
            if (cost > book.getCash() + 0.001) continue;

            AgentPosition position = positionRepository.findById(ticker).orElseGet(() -> {
                AgentPosition p = new AgentPosition();
                p.setTicker(ticker);
                p.setShares(0);
                p.setAvgCost(0);
                p.setCostBasis(0);
                return p;
            });

            double newShares = position.getShares() + shares;
            double newBasis = position.getCostBasis() + cost;
            position.setShares(newShares);
            position.setCostBasis(newBasis);
            position.setAvgCost(newBasis / newShares);
            position.setMarkPrice(price);
            position.setMarkedAt(Instant.now());
            position.setUpdatedAt(Instant.now());
            positionRepository.save(position);

            book.setCash(round2(book.getCash() - cost));
            book.setUpdatedAt(Instant.now());
        }
        portfolioRepository.save(book);
        PortfolioSnapshot snap = snapshot();
        recordMark(snap);
        return snap;
    }

    @Transactional
    public void updateMarks(Map<String, Double> marks) {
        if (marks == null || marks.isEmpty()) return;
        Instant now = Instant.now();
        for (Map.Entry<String, Double> entry : marks.entrySet()) {
            positionRepository.findById(entry.getKey()).ifPresent(position -> {
                Double price = entry.getValue();
                if (price == null || price <= 0) return;
                position.setMarkPrice(price);
                position.setMarkedAt(now);
                position.setUpdatedAt(now);
                positionRepository.save(position);
            });
        }
        getOrCreate().setUpdatedAt(now);
    }

    @Transactional(readOnly = true)
    public PortfolioSnapshot snapshot() {
        AgentPortfolio book = getOrCreate();
        PortfolioSnapshot snap = new PortfolioSnapshot();
        snap.startingCashSeed = book.getStartingCash();
        snap.cash = round2(book.getCash());
        snap.dataFeed = "iex";

        List<AgentPosition> positions = positionRepository.findAll();
        double investedCost = 0;
        double holdingsValue = 0;
        Instant now = Instant.now();

        for (AgentPosition row : positions) {
            if (row.getShares() <= 0) continue;
            PortfolioSnapshot.PositionView view = new PortfolioSnapshot.PositionView();
            view.ticker = row.getTicker();
            view.shares = row.getShares();
            view.avgCost = round2(row.getAvgCost());
            view.costBasis = round2(row.getCostBasis());
            view.markPrice = row.getMarkPrice() == null ? null : round2(row.getMarkPrice());
            view.markedAt = row.getMarkedAt();
            double mark = view.markPrice != null ? view.markPrice : view.avgCost;
            view.marketValue = round2(row.getShares() * mark);
            view.unrealizedPnl = round2(view.marketValue - view.costBasis);
            view.unrealizedPnlPct = view.costBasis > 0
                    ? round4(view.unrealizedPnl / view.costBasis) : null;
            view.live = row.getMarkedAt() != null
                    && row.getMarkedAt().isAfter(now.minusSeconds(120));
            snap.positions.add(view);
            investedCost += view.costBasis;
            holdingsValue += view.marketValue;
        }

        snap.investedCost = round2(investedCost);
        snap.holdingsValue = round2(holdingsValue);
        snap.equity = round2(snap.cash + snap.holdingsValue);
        snap.totalPnl = round2(snap.equity - snap.startingCashSeed);
        snap.totalPnlPct = snap.startingCashSeed > 0
                ? round4(snap.totalPnl / snap.startingCashSeed) : 0;
        snap.unrealizedPnl = round2(holdingsValue - investedCost);
        snap.realizedPnl = 0;
        snap.asOf = now;
        return snap;
    }

    @Transactional
    public PortfolioSnapshot reset() {
        positionRepository.deleteAll();
        AgentPortfolio book = getOrCreate();
        book.setStartingCash(startingCashSeed);
        book.setCash(startingCashSeed);
        book.setUpdatedAt(Instant.now());
        portfolioRepository.save(book);
        markRepository.deleteAll();
        PortfolioSnapshot snap = snapshot();
        recordMark(snap);
        return snap;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> historyPoints() {
        return markRepository.findTop500ByOrderByTakenAtAsc().stream()
                .map(row -> {
                    Map<String, Object> point = new LinkedHashMap<>();
                    point.put("t", row.getTakenAt().toString());
                    point.put("equity", round2(row.getEquity()));
                    point.put("totalPnl", round2(row.getTotalPnl()));
                    return point;
                })
                .toList();
    }

    @Transactional
    public void recordMark(PortfolioSnapshot snap) {
        AgentPortfolioMark mark = new AgentPortfolioMark();
        mark.setEquity(snap.equity);
        mark.setTotalPnl(snap.totalPnl);
        mark.setTakenAt(Instant.now());
        markRepository.save(mark);
        trimHistory();
    }

    private void trimHistory() {
        List<AgentPortfolioMark> marks = markRepository.findTop500ByOrderByTakenAtAsc();
        if (marks.size() <= 500) return;
        markRepository.deleteAll(marks.subList(0, marks.size() - 500));
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static double round4(double v) {
        return Math.round(v * 10_000.0) / 10_000.0;
    }
}
