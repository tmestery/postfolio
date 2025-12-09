package com.postfolio.postfolio.models.post;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.SequenceGenerator;
import com.postfolio.postfolio.models.user.WebUser;
import lombok.Getter;
import jakarta.persistence.Id;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.ManyToOne;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;

@Getter
@Setter
@Entity
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "post_seq")
    @SequenceGenerator(name = "post_seq", sequenceName = "post_seq", allocationSize = 1)
    private Long id;
    private LocalDate dateInvested;
    private LocalDateTime datePosted = LocalDateTime.now();
    private String stock;
    private double shares;
    private double pricePerShare;
    private double investedAmount;

    @ManyToOne(optional = false)
    @JsonIgnoreProperties({"password", "dateOfBirth", "firstName", "lastName", "email"})
    @JoinColumn(name = "user_id")
    private WebUser user;

    public LocalDate getDateInvested() {
        return dateInvested;
    }

    public void setCreatedAt(LocalDateTime datePosted) {
        this.datePosted = datePosted;
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

    public void setPricePerShare(double pricePerShare) {
        this.pricePerShare = pricePerShare;
    }

    public double getShares() {
        return shares;
    }

    public void setShares(double shares) {
        this.shares = shares;
    }

    public String getStock() {
        return stock;
    }

    public void setStock(String stock) {
        this.stock = stock;
    }
}