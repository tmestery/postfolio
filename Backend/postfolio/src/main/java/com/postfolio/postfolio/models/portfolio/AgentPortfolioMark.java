package com.postfolio.postfolio.models.portfolio;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Equity snapshot for the agent desk chart (docs/agent-trader-v4.md). */
@Getter
@Setter
@Entity
@Table(name = "agent_portfolio_mark")
public class AgentPortfolioMark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double equity;
    private double totalPnl;
    private Instant takenAt = Instant.now();
}
