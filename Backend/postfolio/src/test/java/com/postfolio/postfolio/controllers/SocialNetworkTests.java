package com.postfolio.postfolio.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.postfolio.postfolio.models.user.UserRepository;
import com.postfolio.postfolio.models.user.WebUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SocialNetworkTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void seedUsers() {
        createUser("alice", true);
        createUser("bob", true);
        createUser("pat", false);
    }

    private void createUser(String username, boolean isPublic) {
        if (userRepository.findByUsername(username).isPresent()) return;
        WebUser user = new WebUser();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setAccountStatus(isPublic);
        userRepository.save(user);
    }

    private String followBody(String actor, String target) throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("username", actor);
        body.put("targetUsername", target);
        return objectMapper.writeValueAsString(body);
    }

    private String respondBody(String owner, String requester) throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("username", owner);
        body.put("requesterUsername", requester);
        return objectMapper.writeValueAsString(body);
    }

    // Positive: public follow is accepted immediately and notifies the target.
    @Test
    void followPublicUserIsAcceptedAndNotifies() throws Exception {
        mockMvc.perform(post("/social/follow/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(followBody("alice", "bob")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("accepted"));

        mockMvc.perform(get("/notifications/").param("username", "bob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("follow"))
                .andExpect(jsonPath("$[0].actorUsername").value("alice"));
    }

    // Negative: cannot follow yourself.
    @Test
    void selfFollowIsRejected() throws Exception {
        mockMvc.perform(post("/social/follow/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(followBody("alice", "alice")))
                .andExpect(status().isBadRequest());
    }

    // Negative: unknown target → 404.
    @Test
    void followUnknownUserReturns404() throws Exception {
        mockMvc.perform(post("/social/follow/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(followBody("alice", "nobody")))
                .andExpect(status().isNotFound());
    }

    // Negative: duplicate follow is idempotent 200.
    @Test
    void duplicateFollowIsIdempotent() throws Exception {
        mockMvc.perform(post("/social/follow/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(followBody("alice", "bob")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/social/follow/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(followBody("alice", "bob")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("accepted"));
    }

    // Positive: private account creates a pending request.
    @Test
    void privateFollowCreatesPendingRequest() throws Exception {
        mockMvc.perform(post("/social/follow/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(followBody("alice", "pat")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("pending"));

        mockMvc.perform(get("/social/follow/requests/").param("username", "pat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].followerUsername").value("alice"));
    }

    // Positive: owner can accept a request.
    @Test
    void ownerCanAcceptFollowRequest() throws Exception {
        mockMvc.perform(post("/social/follow/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(followBody("alice", "pat")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/social/follow/accept/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(respondBody("pat", "alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("accepted"));

        mockMvc.perform(get("/notifications/").param("username", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("follow_accepted"));
    }

    // Negative: non-owner cannot accept (no pending from their perspective as followee).
    @Test
    void nonOwnerAcceptFails() throws Exception {
        mockMvc.perform(post("/social/follow/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(followBody("alice", "pat")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/social/follow/accept/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(respondBody("bob", "alice")))
                .andExpect(status().isNotFound());
    }

    // Negative: decline removes pending; requester still cannot view posts.
    @Test
    void declineRemovesRequestAndKeepsPostsPrivate() throws Exception {
        mockMvc.perform(post("/social/follow/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(followBody("alice", "pat")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/social/follow/decline/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(respondBody("pat", "alice")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/pat/posts/").param("viewer", "alice"))
                .andExpect(status().isForbidden());
    }

    // Positive: following feed includes accepted followee posts + self.
    @Test
    void followingFeedIncludesAcceptedFolloweePosts() throws Exception {
        mockMvc.perform(post("/social/follow/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(followBody("alice", "bob")))
                .andExpect(status().isCreated());

        Map<String, Object> post = new HashMap<>();
        post.put("username", "bob");
        post.put("stock", "NVDA");
        post.put("shares", 2);
        post.put("investedAmount", 200);
        post.put("dateInvested", "2026-07-01");

        mockMvc.perform(post("/post/stock/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(post)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/post/feed/")
                        .param("username", "alice")
                        .param("mode", "following"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stock").value("NVDA"));

        mockMvc.perform(get("/notifications/").param("username", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("followed_post"));
    }

    // Negative: pending private follow does not expose posts or following feed items.
    @Test
    void pendingPrivateFollowDoesNotShowPosts() throws Exception {
        mockMvc.perform(post("/social/follow/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(followBody("alice", "pat")))
                .andExpect(status().isCreated());

        Map<String, Object> post = new HashMap<>();
        post.put("username", "pat");
        post.put("stock", "AAPL");
        post.put("shares", 1);
        post.put("investedAmount", 100);
        post.put("dateInvested", "2026-07-01");
        mockMvc.perform(post("/post/stock/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(post)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/users/pat/posts/").param("viewer", "alice"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/post/feed/")
                        .param("username", "alice")
                        .param("mode", "following"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // Negative: discover excludes private authors.
    @Test
    void discoverExcludesPrivateAuthors() throws Exception {
        Map<String, Object> post = new HashMap<>();
        post.put("username", "pat");
        post.put("stock", "MSFT");
        post.put("shares", 1);
        post.put("investedAmount", 100);
        post.put("dateInvested", "2026-07-01");
        mockMvc.perform(post("/post/stock/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(post)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/post/feed/").param("mode", "discover"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.stock == 'MSFT')]").isEmpty());
    }

    // Negative: following mode without username → 400.
    @Test
    void followingFeedRequiresUsername() throws Exception {
        mockMvc.perform(get("/post/feed/").param("mode", "following"))
                .andExpect(status().isBadRequest());
    }
}
