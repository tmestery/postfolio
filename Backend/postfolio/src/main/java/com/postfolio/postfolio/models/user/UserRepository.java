package com.postfolio.postfolio.models.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<WebUser, Long> {

    Optional<WebUser> findByUsername(String username);

    @Modifying
    @Query("UPDATE WebUser u SET u.accountPublicStatus = :status WHERE u.id = :userId")
    int setAccountStatus(Long userId, boolean status);
}