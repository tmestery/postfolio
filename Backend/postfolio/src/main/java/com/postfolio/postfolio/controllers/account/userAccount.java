package com.postfolio.postfolio.controllers.loginSignup;

import com.postfolio.postfolio.models.user.UserRepository;
import com.postfolio.postfolio.models.user.WebUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
@RequestMapping("/account")
public class userAccount {
    private UserRepository userRepository;

    /**
     * POST https://localhost:8080/account/status/
     *
     * @param accountPublic
     */
    @PostMapping("/status/")
    public void setAccountStatus(Long userId, boolean accountPublic) {
        userRepository.setAccountStatus(userId, accountPublic);
    }
}