package com.postfolio.postfolio.controllers.post;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import com.postfolio.postfolio.models.post.PostRepository;
import com.postfolio.postfolio.models.post.Post;
import com.postfolio.postfolio.models.post.PostService;
import com.postfolio.postfolio.models.user.WebUser;
import org.springframework.http.HttpStatus;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDateTime;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.List;
import java.util.Optional;
import com.postfolio.postfolio.models.user.UserRepository;
import com.postfolio.postfolio.models.user.WebUser;

@RestController
@RequestMapping("/post")
public class postController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository repository;

    private PostService postService;

    public postController(PostService postService) {
        this.postService = postService;
    }

    public static class StockPostRequest {
        public LocalDate dateInvested;
        public String stock;
        public Double shares;
        public Double investedAmount;
    }

    /**
     * POST http://localhost:8080/post/stock/search/
     *
     * @return a list of posts that contain that stock (***feature not specific to user currently)
     */
    @PostMapping("/stock/search/")
    @ResponseBody
    public ResponseEntity<List<Post>> searchForStocks(@RequestParam String stockName) {
        List<Post> posts = postService.getPostsByStock(stockName);
        if (!posts.isEmpty()) {
            return ResponseEntity.ok(posts);
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * POST http://localhost:8080/post/stock/
     *
     * @return a success or failure code
     */
    @PostMapping("/stock/")
    @ResponseBody
    public ResponseEntity<Post> createStockPost(@AuthenticationPrincipal WebUser user, @RequestBody StockPostRequest request) {

        Post post = postService.createPost(user, request.dateInvested, request.stock, request.shares, request.investedAmount);
        post.setUser(user);
        return new ResponseEntity<>(post, HttpStatus.CREATED);
    }

    /**
     * JUST FOR TESTING PURPOSES!!!
     */
    @PostMapping("/stock/test/")
    public ResponseEntity<Post> createStockPostTest(@RequestBody StockPostRequest request) {
        WebUser user = userRepository.findById(1L).orElseThrow(); // fetch user manually
        Post post = postService.createPost(user, request.dateInvested, request.stock, request.shares, request.investedAmount);
        return new ResponseEntity<>(post, HttpStatus.CREATED);
    }

    /**
     * POST http://localhost:8080/post/feed/
     *
     * @return
     */
    @GetMapping("/feed/")
    public List<Post> feed() {
        return postService.getFeed();
    }
}
