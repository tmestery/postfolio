package com.postfolio.postfolio.models.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentPortfolioMarkRepository extends JpaRepository<AgentPortfolioMark, Long> {

    List<AgentPortfolioMark> findTop500ByOrderByTakenAtAsc();
}
