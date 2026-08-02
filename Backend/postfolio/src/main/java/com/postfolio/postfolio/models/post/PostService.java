package com.postfolio.postfolio.models.post;

import org.springframework.stereotype.Service;
import com.postfolio.postfolio.models.user.WebUser;
import java.util.List;
import java.util.Optional;
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
        post.setPricePerShare(shares > 0 ? investedAmount / shares : 0);
        return repository.save(post);
    }

    /** Feed only shows posts from public accounts. */
    public List<Post> getFeed() {
        return repository.findAllByUserAccountPublicStatusTrueOrderByDatePostedDesc();
    }

    public Optional<Post> findById(Long postId) {
        return repository.findById(postId);
    }

    public void deletePost(Long postId) {
        repository.deleteById(postId);
    }
}
