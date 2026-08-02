package com.postfolio.postfolio.controllers.account;

import com.postfolio.postfolio.models.user.UserRepository;
import com.postfolio.postfolio.models.user.UserService;
import com.postfolio.postfolio.models.user.WebUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/account")
public class userAccount {

    private final UserRepository userRepository;
    private final UserService userService;

    public userAccount(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    public static class AccountStatusRequest {
        public String username;
        public Boolean accountPublic;
    }

    /**
     * GET http://localhost:8080/account/status/?username=demo
     *
     * @return 200 + current visibility, 404 unknown user
     */
    @GetMapping("/status/")
    public ResponseEntity<?> getAccountStatus(@RequestParam String username) {
        Optional<WebUser> user = userRepository.findByUsername(username);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "unknown user: " + username));
        }
        return ResponseEntity.ok(statusBody(user.get()));
    }

    /**
     * POST http://localhost:8080/account/status/
     * Body: {"username": "demo", "accountPublic": false}
     *
     * @return 200 + updated visibility, 400 missing fields, 404 unknown user
     */
    @PostMapping(value = "/status/", consumes = "application/json")
    public ResponseEntity<?> setAccountStatus(@RequestBody AccountStatusRequest request) {
        if (request.username == null || request.username.isBlank() || request.accountPublic == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "username and accountPublic are required"));
        }
        Optional<WebUser> user = userRepository.findByUsername(request.username);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "unknown user: " + request.username));
        }
        userService.setUserAccountStatus(user.get().getId(), request.accountPublic);
        user.get().setAccountStatus(request.accountPublic);
        return ResponseEntity.ok(statusBody(user.get()));
    }

    private static Map<String, Object> statusBody(WebUser user) {
        return Map.of(
                "username", user.getUsername(),
                "accountPublicStatus", user.getaccountPublicStatus());
    }
}
