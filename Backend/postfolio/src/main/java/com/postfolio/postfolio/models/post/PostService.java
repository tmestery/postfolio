package com.postfolio.postfolio.models.post;

import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import com.postfolio.postfolio.models.post.PostRepository;
import com.postfolio.postfolio.models.post.Post;
import com.postfolio.postfolio.models.user.WebUser;
import java.util.Optional;
import java.util.List;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Service
public class PostService {

    private final PostRepository repository;

    public PostService(PostRepository repository) {
        this.repository = repository;
    }

    public List<Post> getPostsByStock(String stock) {
        return repository.findAllByStock(stock);
    }

    public Post createPost(WebUser user, LocalDate dateInvested, String stock, Double shares, Double investedAmount) {
        Post post = new Post();
        post.setUser(user);
        post.setDateInvested(dateInvested);
        post.setCreatedAt(LocalDateTime.now());
        post.setStock(stock);
        post.setShares(shares);
        post.setInvestedAmount(investedAmount);
        return repository.save(post);
    }

    public List<Post> getFeed() {
        return repository.findAllByOrderByDatePostedDesc();
    }
}