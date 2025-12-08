package com.postfolio.postfolio.models;

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
    private LocalDate datePosted;

    public LocalDate getDatePosted() {
        return datePosted;
    }

    public void setDatePosted(LocalDate datePosted) {
        this.datePosted = datePosted;
    }
}