package com.postfolio.postfolio.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.SequenceGenerator;
import lombok.Getter;
import jakarta.persistence.Id;
import lombok.Setter;

@Getter
@Setter
@Entity
public class WebUser {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "web_user_seq_gen")
    @SequenceGenerator(name = "web_user_seq_gen", sequenceName = "web_user_seq", allocationSize = 1)
    private Long id;
    private String username;
    private String email;
    private String password;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}