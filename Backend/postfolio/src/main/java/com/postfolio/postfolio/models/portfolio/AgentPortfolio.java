package com.postfolio.postfolio.models.portfolio;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Singleton house paper book (docs/agent-trader-v4.md). */
@Getter
@Setter
@Entity
@Table(name = "agent_portfolio")
public class AgentPortfolio {

    public static final short SINGLETON_ID = 1;

    @Id
    private Short id = SINGLETON_ID;

    private double startingCash;
    private double cash;
    private Instant updatedAt = Instant.now();
}
