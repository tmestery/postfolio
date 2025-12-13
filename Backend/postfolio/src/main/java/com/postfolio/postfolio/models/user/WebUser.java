package com.postfolio.postfolio.models.user;

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
public class WebUser {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "web_user_seq_gen")
    @SequenceGenerator(name = "web_user_seq_gen", sequenceName = "web_user_seq", allocationSize = 1)
    private Long id;
    private String username;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth; // (yyyy-MM-dd)
    private Boolean accountPublicStatus; // account public or private

    public Boolean getaccountPublicStatus() {
        return accountPublicStatus;
    }

    public void setAccountStatus(Boolean accountPublicStatus) {
        this.accountPublicStatus = accountPublicStatus;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
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

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
}