package com.postfolio.postfolio.controllers.post;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import com.postfolio.postfolio.models.post.PostRepository;
import com.postfolio.postfolio.models.post.Post;
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

    /**
     * POST http://localhost:8080/post/stock/search/
     *
     * @return a list of posts that contain that stock (search feature not specific to user currently)
     */
    @PostMapping("/stock/search/")
    @ResponseBody
    public ResponseEntity<List<Post>> searchForStocks(String stockName) {
        List<Post> posts = repository.findAllByStock(stockName);
        System.out.println("Finding Posts!");

        if (posts.size() > 0) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(posts);
    }

    /**
     * POST http://localhost:8080/post/stock/
     *
     * @return
     */
    @PostMapping("/stock/")
    @ResponseBody
    public String searchForStocks(String username, LocalDate dateInvested, String stockName, double shares, double investedAmount) {
        return "Creating Post!";
    }
}