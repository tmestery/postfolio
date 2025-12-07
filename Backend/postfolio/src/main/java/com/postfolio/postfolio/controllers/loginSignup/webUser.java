package com.postfolio.postfolio.controllers.loginSignup;

import com.postfolio.postfolio.models.UserRepository;
import com.postfolio.postfolio.models.WebUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
@RequestMapping("/credentials")
public class webUser {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // POST http://localhost:8080/credentials/signup
    @PostMapping(value = "/signup/", consumes = "application/json")
    public WebUser createUser(@RequestBody WebUser user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        System.out.println("User Signup Complete!");
        return userRepository.save(user);
    }

    // POST http://localhost:8080/credentials/login/
    @PostMapping("/login/")
    @ResponseBody
    public ResponseEntity login(@RequestBody WebUser user) {
        Optional<WebUser> dbUser = userRepository.findByUsername(user.getUsername());
        if (dbUser.isPresent() && passwordEncoder.matches(user.getPassword(), dbUser.get().getPassword())) {
            String getUser = dbUser.get().getUsername();
            System.out.println("User Login Complete!");
            return new ResponseEntity<>(getUser, HttpStatus.ACCEPTED);
        } else {
            String failBody = "Failed!";
            System.out.println("User Login Failed!");
            return new ResponseEntity<>(failBody, HttpStatus.BAD_REQUEST);
        }
    }
}