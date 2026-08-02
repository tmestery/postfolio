package com.postfolio.postfolio.models.follow;

import com.postfolio.postfolio.models.user.WebUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {

    Optional<Follow> findByFollowerAndFollowee(WebUser follower, WebUser followee);

    List<Follow> findByFollowerAndStatus(WebUser follower, FollowStatus status);

    List<Follow> findByFolloweeAndStatus(WebUser followee, FollowStatus status);

    long countByFolloweeAndStatus(WebUser followee, FollowStatus status);

    long countByFollowerAndStatus(WebUser follower, FollowStatus status);

    boolean existsByFollowerAndFolloweeAndStatus(WebUser follower, WebUser followee, FollowStatus status);
}
