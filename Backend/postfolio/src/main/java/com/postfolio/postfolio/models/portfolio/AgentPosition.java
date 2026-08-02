package com.postfolio.postfolio.models.portfolio;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "agent_position")
public class AgentPosition {

    @Id
    private String ticker;

    private double shares;
    private double avgCost;
    private double costBasis;
    private Double markPrice;
    private Instant markedAt;
    private Instant updatedAt = Instant.now();
}
