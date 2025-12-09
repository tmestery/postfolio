package com.postfolio.postfolio.models;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.postfolio.postfolio.models.post.PostRepository;
import com.postfolio.postfolio.models.post.Post;
import java.util.Optional;
import java.util.List;

@Service
public class PostService {

    private final PostRepository repository;

    public PostService(PostRepository repository) {
        this.repository = repository;
    }

    public List<Post> getPostsByStock(String stock) {
        return repository.findAllByStock(stock);
    }
}