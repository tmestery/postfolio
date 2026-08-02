package com.postfolio.postfolio.models.notification;

import com.postfolio.postfolio.models.user.WebUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop30ByRecipientOrderByCreatedAtDesc(WebUser recipient);

    long countByRecipientAndReadAtIsNull(WebUser recipient);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.readAt = :when WHERE n.recipient = :recipient AND n.readAt IS NULL")
    int markAllRead(@Param("recipient") WebUser recipient, @Param("when") LocalDateTime when);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.readAt = :when WHERE n.recipient = :recipient AND n.id IN :ids AND n.readAt IS NULL")
    int markIdsRead(@Param("recipient") WebUser recipient,
                    @Param("ids") Collection<Long> ids,
                    @Param("when") LocalDateTime when);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.readAt = :when WHERE n.followId = :followId AND n.type = 'follow_request' AND n.readAt IS NULL")
    int markFollowRequestRead(@Param("followId") Long followId, @Param("when") LocalDateTime when);
}
