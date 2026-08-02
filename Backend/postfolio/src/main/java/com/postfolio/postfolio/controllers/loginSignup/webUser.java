package com.postfolio.postfolio.controllers.loginSignup;

import com.postfolio.postfolio.models.user.UserRepository;
import com.postfolio.postfolio.models.user.WebUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/credentials")
public class webUser {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * POST http://localhost:8080/credentials/signup/
     *
     * Required: username, email, password. Optional: firstName, lastName.
     * accountPublicStatus defaults to public when omitted (locked demo decision).
     *
     * @return 201 + created user (password never serialized), 400 on missing
     *         fields, 409 on duplicate username/email
     */
    @PostMapping(value = "/signup/", consumes = "application/json")
    public ResponseEntity<?> createUser(@RequestBody WebUser user) {
        if (isBlank(user.getUsername()) || isBlank(user.getEmail()) || isBlank(user.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "username, email, and password are required"));
        }
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "username is already taken"));
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "an account with that email already exists"));
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getaccountPublicStatus() == null) {
            user.setAccountStatus(true);
        }
        WebUser saved = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * POST http://localhost:8080/credentials/login/
     *
     * @return 202 + plain-text username on success (documented contract),
     *         400 otherwise
     */
    @PostMapping("/login/")
    @ResponseBody
    public ResponseEntity<String> login(@RequestBody WebUser user) {
        if (isBlank(user.getUsername()) || isBlank(user.getPassword())) {
            return new ResponseEntity<>("Failed!", HttpStatus.BAD_REQUEST);
        }
        Optional<WebUser> dbUser = userRepository.findByUsername(user.getUsername());
        if (dbUser.isPresent() && passwordEncoder.matches(user.getPassword(), dbUser.get().getPassword())) {
            return new ResponseEntity<>(dbUser.get().getUsername(), HttpStatus.ACCEPTED);
        }
        return new ResponseEntity<>("Failed!", HttpStatus.BAD_REQUEST);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
