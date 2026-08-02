package com.postfolio.postfolio.models.symbol;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock_symbol")
public class StockSymbol {

    @Id
    @Column(length = 16, nullable = false)
    private String symbol;

    @Column(nullable = false)
    private String name;

    public StockSymbol() {
    }

    public StockSymbol(String symbol, String name) {
        this.symbol = symbol;
        this.name = name;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
