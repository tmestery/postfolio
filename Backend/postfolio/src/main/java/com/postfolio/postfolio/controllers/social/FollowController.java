package com.postfolio.postfolio.controllers.social;

import com.postfolio.postfolio.models.follow.Follow;
import com.postfolio.postfolio.models.follow.FollowService;
import com.postfolio.postfolio.models.user.WebUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/social")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    public static class FollowRequest {
        public String username;
        public String targetUsername;
    }

    public static class RespondRequest {
        public String username;
        public String requesterUsername;
    }

    @PostMapping("/follow/")
    public ResponseEntity<?> follow(@RequestBody FollowRequest body) {
        FollowService.FollowResult result = followService.follow(body.username, body.targetUsername);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(followService.toEdgeDto(result.follow()));
    }

    @PostMapping("/unfollow/")
    public ResponseEntity<Void> unfollow(@RequestBody FollowRequest body) {
        followService.unfollow(body.username, body.targetUsername);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/follow/accept/")
    public ResponseEntity<?> accept(@RequestBody RespondRequest body) {
        Follow follow = followService.accept(body.username, body.requesterUsername);
        return ResponseEntity.ok(followService.toEdgeDto(follow));
    }

    @PostMapping("/follow/decline/")
    public ResponseEntity<Void> decline(@RequestBody RespondRequest body) {
        followService.decline(body.username, body.requesterUsername);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/follow/requests/")
    public List<Map<String, Object>> requests(@RequestParam String username) {
        return followService.pendingRequests(username).stream().map(f -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("followId", f.getId());
            row.put("followerUsername", f.getFollower().getUsername());
            row.put("createdAt", f.getCreatedAt() != null ? f.getCreatedAt().toString() : null);
            return row;
        }).toList();
    }

    @GetMapping("/following/")
    public List<Map<String, Object>> following(@RequestParam String username) {
        return followService.following(username).stream().map(followService::toUserSummary).toList();
    }

    @GetMapping("/followers/")
    public List<Map<String, Object>> followers(@RequestParam String username,
                                               @RequestParam(required = false) String viewer) {
        return followService.followers(username, viewer).stream().map(followService::toUserSummary).toList();
    }

    @GetMapping("/follows/status/")
    public Map<String, String> status(@RequestParam String username,
                                      @RequestParam String targetUsername) {
        return Map.of("status", followService.relationshipStatus(username, targetUsername));
    }
}
