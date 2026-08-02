package com.postfolio.postfolio.models.agentrun;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** One persisted agent run (docs/agent-trader-v2.md §10). */
@Getter
@Setter
@Entity
@Table(name = "agent_run")
public class AgentRun {

    @Id
    private UUID id;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDateTime createdAt = LocalDateTime.now();

    private String username;
    private String status;
    private double startingAllowance;
    private double totalInvested;
    private double remainingAllowance;

    @Column(columnDefinition = "TEXT")
    private String error;

    @Column(columnDefinition = "TEXT")
    private String resultJson;
}
