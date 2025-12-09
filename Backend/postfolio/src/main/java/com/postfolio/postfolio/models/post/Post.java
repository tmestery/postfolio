package com.postfolio.postfolio.models.post;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.SequenceGenerator;
import lombok.Getter;
import jakarta.persistence.Id;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
@Entity
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "post_seq_gen")
    @SequenceGenerator(name = "post_seq", sequenceName = "post_seq", allocationSize = 1)
    private Long id;
    private LocalDate dateInvested;
    private LocalDate datePosted;
    private String stock;
    private double shares;
    private double pricePerShare;
    private double investedAmount;

    public LocalDate getDateInvested() {
        return dateInvested;
    }

    public void setDateInvested(LocalDate dateInvested) {
        this.dateInvested = dateInvested;
    }

    public double getInvestedAmount() {
        return investedAmount;
    }

    public void setInvestedAmount(double investedAmount) {
        this.investedAmount = investedAmount;
    }

    public double getPricePerShare() {
        return pricePerShare;
    }

    public void setPricePerShare() {
        this.pricePerShare = pricePerShare;
    }

   public double getShares() {
       return shares;
   }

   public void setShares(double shares) {
       this.shares = shares;
   }

    public String getStockName() {
        return stock;
    }

    public void setStockName(String stock) {
        this.stock = stock;
    }

    public LocalDate getDatePosted() {
        return datePosted;
    }

    public void setDatePosted(LocalDate datePosted) {
        this.datePosted = datePosted;
    }
}