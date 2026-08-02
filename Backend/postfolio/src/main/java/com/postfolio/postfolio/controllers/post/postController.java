package com.postfolio.postfolio.controllers.post;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import com.postfolio.postfolio.models.post.Post;
import com.postfolio.postfolio.models.post.PostService;
import com.postfolio.postfolio.models.user.UserRepository;
import com.postfolio.postfolio.models.user.WebUser;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/post")
public class postController {

    private final UserRepository userRepository;
    private final PostService postService;

    public postController(PostService postService, UserRepository userRepository) {
        this.postService = postService;
        this.userRepository = userRepository;
    }

    public static class StockPostRequest {
        public LocalDate dateInvested;
        public String stock;
        public Double shares;
        public Double investedAmount;
        /** Demo bridge: identifies the author while there is no server session. */
        public String username;
    }

    /**
     * POST http://localhost:8080/post/stock/search/?stockName=AAPL
     *
     * @return 200 + matching posts (empty list when none)
     */
    @PostMapping("/stock/search/")
    public ResponseEntity<List<Post>> searchForStocks(@RequestParam String stockName) {
        return ResponseEntity.ok(postService.getPostsByStock(stockName.trim().toUpperCase()));
    }

    /**
     * POST http://localhost:8080/post/stock/
     *
     * Uses the authenticated principal when present; otherwise resolves the
     * author from {@code username} in the body (locked demo bridge).
     *
     * @return 201 + created post, 400 on validation/unknown user
     */
    @PostMapping("/stock/")
    public ResponseEntity<?> createStockPost(@AuthenticationPrincipal WebUser principal,
                                             @RequestBody StockPostRequest request) {
        WebUser author = principal;
        if (author == null) {
            if (request.username == null || request.username.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "username is required"));
            }
            Optional<WebUser> resolved = userRepository.findByUsername(request.username);
            if (resolved.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "unknown user: " + request.username));
            }
            author = resolved.get();
        }

        if (request.stock == null || request.stock.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "stock ticker is required"));
        }
        if (request.shares == null || request.shares <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "shares must be greater than 0"));
        }
        if (request.investedAmount == null || request.investedAmount <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "investedAmount must be greater than 0"));
        }

        Post post = postService.createPost(
                author,
                request.dateInvested,
                request.stock.trim().toUpperCase(),
                request.shares,
                request.investedAmount);
        return new ResponseEntity<>(post, HttpStatus.CREATED);
    }

    /**
     * GET http://localhost:8080/post/feed/
     *
     * @return 200 + newest-first posts from public accounts
     */
    @GetMapping("/feed/")
    public List<Post> feed() {
        return postService.getFeed();
    }

    /**
     * POST http://localhost:8080/post/delete/?postId=1&username=demo
     *
     * Only the post owner may delete (username demo bridge).
     *
     * @return 204 deleted, 403 not owner, 404 unknown post, 400 missing params
     */
    @PostMapping("/delete/")
    public ResponseEntity<?> deletePost(@RequestParam Long postId, @RequestParam String username) {
        Optional<Post> post = postService.findById(postId);
        if (post.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "post not found"));
        }
        if (post.get().getUser() == null || !username.equals(post.get().getUser().getUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "only the post owner can delete it"));
        }
        postService.deletePost(postId);
        return ResponseEntity.noContent().build();
    }
}
