package com.postfolio.postfolio.models.notification;

import com.postfolio.postfolio.models.follow.Follow;
import com.postfolio.postfolio.models.post.Post;
import com.postfolio.postfolio.models.user.WebUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Notification create(WebUser recipient, WebUser actor, String type, String message,
                               Post post, Follow follow) {
        Notification n = new Notification();
        n.setRecipient(recipient);
        n.setActor(actor);
        n.setType(type);
        n.setMessage(message);
        n.setPost(post);
        n.setFollowId(follow != null ? follow.getId() : null);
        return notificationRepository.save(n);
    }

    public List<Notification> listFor(WebUser recipient) {
        return notificationRepository.findTop30ByRecipientOrderByCreatedAtDesc(recipient);
    }

    public long unreadCount(WebUser recipient) {
        return notificationRepository.countByRecipientAndReadAtIsNull(recipient);
    }

    @Transactional
    public void markRead(WebUser recipient, Collection<Long> ids, boolean all) {
        LocalDateTime now = LocalDateTime.now();
        if (all) {
            notificationRepository.markAllRead(recipient, now);
        } else if (ids != null && !ids.isEmpty()) {
            notificationRepository.markIdsRead(recipient, ids, now);
        }
    }

    @Transactional
    public void markFollowRequestRead(Long followId) {
        if (followId != null) {
            notificationRepository.markFollowRequestRead(followId, LocalDateTime.now());
        }
    }

    public Map<String, Object> toDto(Notification n) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", n.getId());
        dto.put("type", n.getType());
        dto.put("message", n.getMessage());
        dto.put("actorUsername", n.getActor() != null ? n.getActor().getUsername() : null);
        dto.put("followId", n.getFollowId());
        dto.put("postId", n.getPost() != null ? n.getPost().getId() : null);
        dto.put("read", n.getReadAt() != null);
        dto.put("createdAt", n.getCreatedAt() != null ? n.getCreatedAt().toString() : null);
        return dto;
    }
}
