package com.postfolio.postfolio.models.post;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.postfolio.postfolio.models.user.WebUser;
import java.util.Optional;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findAllByStock(String stock);
    List<Post> findByUser(WebUser user);
    List<Post> findAllByOrderByDatePostedDesc();
}