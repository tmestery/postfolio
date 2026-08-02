package com.postfolio.postfolio.controllers.social;

import com.postfolio.postfolio.models.follow.FollowService;
import com.postfolio.postfolio.models.post.Post;
import com.postfolio.postfolio.models.post.PostRepository;
import com.postfolio.postfolio.models.user.UserRepository;
import com.postfolio.postfolio.models.user.WebUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class ProfileController {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final FollowService followService;

    public ProfileController(UserRepository userRepository, PostRepository postRepository,
                             FollowService followService) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.followService = followService;
    }

    @GetMapping("/{username}/")
    public Map<String, Object> profile(@PathVariable String username,
                                       @RequestParam(required = false) String viewer) {
        WebUser user = requireUser(username);
        String relationship = viewer == null || viewer.isBlank()
                ? "none"
                : followService.relationshipStatus(viewer, username);
        boolean canView = followService.canViewPosts(user, viewer);

        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("username", user.getUsername());
        dto.put("firstName", user.getFirstName());
        dto.put("lastName", user.getLastName());
        dto.put("accountPublicStatus", Boolean.TRUE.equals(user.getaccountPublicStatus()));
        dto.put("followerCount", followService.followerCount(user));
        dto.put("followingCount", followService.followingCount(user));
        dto.put("viewerRelationship", relationship);
        dto.put("canViewPosts", canView);
        return dto;
    }

    @GetMapping("/{username}/posts/")
    public ResponseEntity<?> posts(@PathVariable String username,
                                   @RequestParam(required = false) String viewer) {
        WebUser user = requireUser(username);
        if (!followService.canViewPosts(user, viewer)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "This account is private"));
        }
        List<Post> posts = postRepository.findByUser(user).stream()
                .sorted((a, b) -> {
                    if (a.getDatePosted() == null || b.getDatePosted() == null) return 0;
                    return b.getDatePosted().compareTo(a.getDatePosted());
                })
                .toList();
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/search/")
    public List<Map<String, Object>> search(@RequestParam String q,
                                            @RequestParam(required = false) String username) {
        String query = q == null ? "" : q.trim().toLowerCase();
        if (query.isEmpty()) return List.of();

        return userRepository.findTop20ByUsernameStartingWithIgnoreCaseOrderByUsernameAsc(query).stream()
                .map(u -> {
                    Map<String, Object> row = followService.toUserSummary(u);
                    row.put("accountPublicStatus", Boolean.TRUE.equals(u.getaccountPublicStatus()));
                    row.put("viewerRelationship",
                            username == null ? "none" : followService.relationshipStatus(username, u.getUsername()));
                    return row;
                })
                .toList();
    }

    private WebUser requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "unknown user: " + username));
    }
}
