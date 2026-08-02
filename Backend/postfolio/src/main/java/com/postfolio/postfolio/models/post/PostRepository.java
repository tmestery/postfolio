package com.postfolio.postfolio.models.post;

import com.postfolio.postfolio.models.user.WebUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findAllByStock(String stock);
    List<Post> findByUser(WebUser user);
    List<Post> findAllByOrderByDatePostedDesc();
    List<Post> findAllByUserAccountPublicStatusTrueOrderByDatePostedDesc();

    @Query("""
            SELECT p FROM Post p
            WHERE p.user.id IN :authorIds
            ORDER BY p.datePosted DESC
            """)
    List<Post> findByAuthorIdsOrderByDatePostedDesc(@Param("authorIds") Collection<Long> authorIds);
}