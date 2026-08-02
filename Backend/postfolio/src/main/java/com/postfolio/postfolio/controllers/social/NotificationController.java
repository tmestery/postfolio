package com.postfolio.postfolio.controllers.social;

import com.postfolio.postfolio.models.notification.NotificationService;
import com.postfolio.postfolio.models.user.UserRepository;
import com.postfolio.postfolio.models.user.WebUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    public static class ReadRequest {
        public String username;
        public List<Long> ids;
        public Boolean all;
    }

    @GetMapping("/")
    public List<Map<String, Object>> list(@RequestParam String username) {
        WebUser user = requireUser(username);
        return notificationService.listFor(user).stream().map(notificationService::toDto).toList();
    }

    @GetMapping("/unread-count/")
    public Map<String, Long> unreadCount(@RequestParam String username) {
        return Map.of("count", notificationService.unreadCount(requireUser(username)));
    }

    @PostMapping("/read/")
    public ResponseEntity<Void> markRead(@RequestBody ReadRequest body) {
        WebUser user = requireUser(body.username);
        notificationService.markRead(user, body.ids, Boolean.TRUE.equals(body.all));
        return ResponseEntity.noContent().build();
    }

    private WebUser requireUser(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username is required");
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "unknown user: " + username));
    }
}
