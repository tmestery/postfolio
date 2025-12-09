package com.postfolio.postfolio.controllers.post;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/post")
public class postController {

    @Autowired
    private PostRepository repository;

    private PostService postService;

    public postController(PostService postService) {
        this.postService = postService;
    }

    /**
     * POST http://localhost:8080/post/stock/search/
     *
     * @return a list of posts that contain that stock (***feature not specific to user currently)
     */
    @PostMapping("/stock/search/")
    @ResponseBody
    public ResponseEntity<List<Post>> searchForStocks(String stockName) {
        List<Post> posts = repository.findAllByStock(stockName);
        System.out.println("Finding Posts!");

        if (posts.size() > 0) {
            return ResponseEntity.ok(posts);
        }

        return ResponseEntity.noContent().build();
    }

    /**
     * POST http://localhost:8080/post/stock/
     *
     * @return a success or failure code (***feature not specific to user currently)
     */
    @PostMapping("/stock/")
    @ResponseBody
    public Post createStockPost(@AuthenticationPrincipal WebUser user, LocalDate dateInvested, String stock, double shares, double investedAmount) {
        return postService.createPost(user, dateInvested, stock, shares, investedAmount);
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