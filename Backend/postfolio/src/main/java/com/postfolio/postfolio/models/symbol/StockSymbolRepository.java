package com.postfolio.postfolio.models.symbol;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockSymbolRepository extends JpaRepository<StockSymbol, String> {

    boolean existsBySymbol(String symbol);

    List<StockSymbol> findBySymbolStartingWithOrderBySymbolAsc(String prefix);
}
