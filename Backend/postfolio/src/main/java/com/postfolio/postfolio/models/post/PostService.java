package com.postfolio.postfolio.models.post;

import com.postfolio.postfolio.models.follow.FollowService;
import com.postfolio.postfolio.models.notification.NotificationService;
import com.postfolio.postfolio.models.symbol.StockSymbolService;
import com.postfolio.postfolio.models.user.WebUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class PostService {

    private final PostRepository repository;
    private final FollowService followService;
    private final NotificationService notificationService;
    private final StockSymbolService stockSymbolService;

    public PostService(PostRepository repository, FollowService followService,
                       NotificationService notificationService,
                       StockSymbolService stockSymbolService) {
        this.repository = repository;
        this.followService = followService;
        this.notificationService = notificationService;
        this.stockSymbolService = stockSymbolService;
    }

    public List<Post> getPostsByStock(String stock) {
        return repository.findAllByStock(stock);
    }

    @Transactional
    public Post createPost(WebUser user, LocalDate dateInvested, String stock, Double shares, Double investedAmount) {
        String ticker = stockSymbolService.normalize(stock);
        if (ticker == null || ticker.isEmpty()) {
            throw new IllegalArgumentException("stock ticker is required");
        }
        if (!stockSymbolService.isKnown(ticker)) {
            throw new IllegalArgumentException("unknown stock ticker: " + ticker);
        }

        Post post = new Post();
        post.setUser(user);
        post.setDateInvested(dateInvested);
        post.setCreatedAt(LocalDateTime.now());
        post.setStock(ticker);
        post.setShares(shares);
        post.setInvestedAmount(investedAmount);
        post.setPricePerShare(shares > 0 ? investedAmount / shares : 0);
        Post saved = repository.save(post);

        // Fan-out to accepted followers only (docs/social-network.md §6.5).
        for (WebUser follower : followService.acceptedFollowers(user)) {
            notificationService.create(
                    follower,
                    user,
                    "followed_post",
                    "@" + user.getUsername() + " posted " + ticker,
                    saved,
                    null);
        }
        return saved;
    }

    /** Discover: posts from public accounts only. */
    public List<Post> getDiscoverFeed() {
        return repository.findAllByUserAccountPublicStatusTrueOrderByDatePostedDesc();
    }

    /** Following: accepted followees ∪ self. */
    public List<Post> getFollowingFeed(WebUser viewer) {
        Set<Long> authorIds = new HashSet<>(followService.acceptedFolloweeIds(viewer));
        authorIds.add(viewer.getId());
        if (authorIds.isEmpty()) return List.of();
        return repository.findByAuthorIdsOrderByDatePostedDesc(new ArrayList<>(authorIds));
    }

    /** @deprecated use getDiscoverFeed — kept name for older call sites. */
    public List<Post> getFeed() {
        return getDiscoverFeed();
    }

    public Optional<Post> findById(Long postId) {
        return repository.findById(postId);
    }

    public void deletePost(Long postId) {
        repository.deleteById(postId);
    }
}
