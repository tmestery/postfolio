package com.postfolio.postfolio.models.follow;

import com.postfolio.postfolio.models.notification.NotificationService;
import com.postfolio.postfolio.models.user.UserRepository;
import com.postfolio.postfolio.models.user.WebUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public FollowService(FollowRepository followRepository, UserRepository userRepository,
                         NotificationService notificationService) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public record FollowResult(Follow follow, boolean created) {}

    @Transactional
    public FollowResult follow(String actorUsername, String targetUsername) {
        WebUser actor = requireUser(actorUsername);
        WebUser target = requireUser(targetUsername);
        if (actor.getId().equals(target.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot follow yourself");
        }

        Optional<Follow> existing = followRepository.findByFollowerAndFollowee(actor, target);
        if (existing.isPresent()) {
            return new FollowResult(existing.get(), false);
        }

        boolean targetPublic = Boolean.TRUE.equals(target.getaccountPublicStatus());
        Follow follow = new Follow();
        follow.setFollower(actor);
        follow.setFollowee(target);
        if (targetPublic) {
            follow.setStatus(FollowStatus.accepted);
            follow.setRespondedAt(LocalDateTime.now());
            follow = followRepository.save(follow);
            notificationService.create(target, actor, "follow",
                    "@" + actor.getUsername() + " followed you", null, follow);
        } else {
            follow.setStatus(FollowStatus.pending);
            follow = followRepository.save(follow);
            notificationService.create(target, actor, "follow_request",
                    "@" + actor.getUsername() + " requested to follow you", null, follow);
        }
        return new FollowResult(follow, true);
    }

    @Transactional
    public void unfollow(String actorUsername, String targetUsername) {
        WebUser actor = requireUser(actorUsername);
        WebUser target = requireUser(targetUsername);
        followRepository.findByFollowerAndFollowee(actor, target).ifPresent(follow -> {
            notificationService.markFollowRequestRead(follow.getId());
            followRepository.delete(follow);
        });
    }

    @Transactional
    public Follow accept(String ownerUsername, String requesterUsername) {
        WebUser owner = requireUser(ownerUsername);
        WebUser requester = requireUser(requesterUsername);
        Follow follow = followRepository.findByFollowerAndFollowee(requester, owner)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "no pending request"));
        if (follow.getStatus() != FollowStatus.pending) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "no pending request");
        }
        follow.setStatus(FollowStatus.accepted);
        follow.setRespondedAt(LocalDateTime.now());
        follow = followRepository.save(follow);
        notificationService.markFollowRequestRead(follow.getId());
        notificationService.create(requester, owner, "follow_accepted",
                "@" + owner.getUsername() + " accepted your follow request", null, follow);
        return follow;
    }

    @Transactional
    public void decline(String ownerUsername, String requesterUsername) {
        WebUser owner = requireUser(ownerUsername);
        WebUser requester = requireUser(requesterUsername);
        followRepository.findByFollowerAndFollowee(requester, owner).ifPresent(follow -> {
            if (follow.getStatus() == FollowStatus.pending) {
                notificationService.markFollowRequestRead(follow.getId());
                followRepository.delete(follow);
            }
        });
    }

    /** When an account becomes public, auto-accept all pending inbound requests (S3c). */
    @Transactional
    public void autoAcceptPendingFor(WebUser newlyPublicUser) {
        List<Follow> pending = followRepository.findByFolloweeAndStatus(newlyPublicUser, FollowStatus.pending);
        LocalDateTime now = LocalDateTime.now();
        for (Follow follow : pending) {
            follow.setStatus(FollowStatus.accepted);
            follow.setRespondedAt(now);
            followRepository.save(follow);
            notificationService.markFollowRequestRead(follow.getId());
            notificationService.create(follow.getFollower(), newlyPublicUser, "follow_accepted",
                    "@" + newlyPublicUser.getUsername() + " accepted your follow request", null, follow);
        }
    }

    public List<Follow> pendingRequests(String ownerUsername) {
        return followRepository.findByFolloweeAndStatus(requireUser(ownerUsername), FollowStatus.pending);
    }

    public List<WebUser> following(String username) {
        return followRepository.findByFollowerAndStatus(requireUser(username), FollowStatus.accepted)
                .stream().map(Follow::getFollowee).toList();
    }

    public List<WebUser> followers(String username, String viewerUsername) {
        WebUser target = requireUser(username);
        if (!canListFollowers(target, viewerUsername)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "followers are hidden");
        }
        return followRepository.findByFolloweeAndStatus(target, FollowStatus.accepted)
                .stream().map(Follow::getFollower).toList();
    }

    public String relationshipStatus(String actorUsername, String targetUsername) {
        if (actorUsername == null || actorUsername.isBlank()) return "none";
        WebUser actor = requireUser(actorUsername);
        WebUser target = requireUser(targetUsername);
        if (actor.getId().equals(target.getId())) return "self";
        return followRepository.findByFollowerAndFollowee(actor, target)
                .map(f -> f.getStatus().name())
                .orElse("none");
    }

    public boolean canViewPosts(WebUser target, String viewerUsername) {
        if (Boolean.TRUE.equals(target.getaccountPublicStatus())) return true;
        if (viewerUsername == null || viewerUsername.isBlank()) return false;
        Optional<WebUser> viewer = userRepository.findByUsername(viewerUsername);
        if (viewer.isEmpty()) return false;
        if (viewer.get().getId().equals(target.getId())) return true;
        return followRepository.existsByFollowerAndFolloweeAndStatus(
                viewer.get(), target, FollowStatus.accepted);
    }

    public long followerCount(WebUser user) {
        return followRepository.countByFolloweeAndStatus(user, FollowStatus.accepted);
    }

    public long followingCount(WebUser user) {
        return followRepository.countByFollowerAndStatus(user, FollowStatus.accepted);
    }

    public List<Long> acceptedFolloweeIds(WebUser follower) {
        return followRepository.findByFollowerAndStatus(follower, FollowStatus.accepted)
                .stream().map(f -> f.getFollowee().getId()).toList();
    }

    public List<WebUser> acceptedFollowers(WebUser followee) {
        return followRepository.findByFolloweeAndStatus(followee, FollowStatus.accepted)
                .stream().map(Follow::getFollower).toList();
    }

    public Map<String, Object> toEdgeDto(Follow follow) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", follow.getId());
        dto.put("follower", follow.getFollower().getUsername());
        dto.put("followee", follow.getFollowee().getUsername());
        dto.put("status", follow.getStatus().name());
        dto.put("createdAt", follow.getCreatedAt() != null ? follow.getCreatedAt().toString() : null);
        return dto;
    }

    public Map<String, Object> toUserSummary(WebUser user) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", user.getId());
        dto.put("username", user.getUsername());
        dto.put("firstName", user.getFirstName());
        dto.put("lastName", user.getLastName());
        return dto;
    }

    private boolean canListFollowers(WebUser target, String viewerUsername) {
        if (Boolean.TRUE.equals(target.getaccountPublicStatus())) return true;
        if (viewerUsername == null) return false;
        if (viewerUsername.equals(target.getUsername())) return true;
        return "accepted".equals(relationshipStatus(viewerUsername, target.getUsername()));
    }

    private WebUser requireUser(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username is required");
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "unknown user: " + username));
    }
}
