package com.postfolio.postfolio.models.follow;

import com.postfolio.postfolio.models.user.WebUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "user_follow", uniqueConstraints = @UniqueConstraint(columnNames = {"follower_id", "followee_id"}))
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "follow_seq")
    @SequenceGenerator(name = "follow_seq", sequenceName = "follow_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "follower_id")
    private WebUser follower;

    @ManyToOne(optional = false)
    @JoinColumn(name = "followee_id")
    private WebUser followee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FollowStatus status = FollowStatus.pending;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime respondedAt;
}
