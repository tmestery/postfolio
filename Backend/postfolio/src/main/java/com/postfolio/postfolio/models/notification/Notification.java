package com.postfolio.postfolio.models.notification;

import com.postfolio.postfolio.models.post.Post;
import com.postfolio.postfolio.models.user.WebUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_seq")
    @SequenceGenerator(name = "notification_seq", sequenceName = "notification_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "recipient_id")
    private WebUser recipient;

    @ManyToOne
    @JoinColumn(name = "actor_id")
    private WebUser actor;

    @Column(nullable = false)
    private String type;

    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;

    /** Loose reference so follow rows can be deleted without cascading notifications. */
    private Long followId;

    @Column(nullable = false)
    private String message;

    private LocalDateTime readAt;
    private LocalDateTime createdAt = LocalDateTime.now();
}
