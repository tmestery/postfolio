package com.postfolio.postfolio.models.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<WebUser, Long> {

    Optional<WebUser> findByUsername(String username);
}