package com.postfolio.postfolio.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.postfolio.postfolio.models.post.Post;
import com.postfolio.postfolio.models.post.PostService;
import com.postfolio.postfolio.models.user.UserRepository;
import com.postfolio.postfolio.models.user.WebUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PostControllerTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostService postService;

    private WebUser createUser(String username, boolean publicAccount) {
        WebUser user = new WebUser();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("encoded");
        user.setAccountStatus(publicAccount);
        return userRepository.save(user);
    }

    private String createPostBody(String username, String stock, Double shares, Double amount) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("stock", stock);
        body.put("shares", shares);
        body.put("investedAmount", amount);
        body.put("dateInvested", "2026-07-01");
        return objectMapper.writeValueAsString(body);
    }

    // --- create ---

    @Test
    void createPostResolvesAuthorByUsernameAndComputesPricePerShare() throws Exception {
        createUser("poster", true);

        mockMvc.perform(post("/post/stock/")
                        .contentType(APPLICATION_JSON)
                        .content(createPostBody("poster", "aapl", 10.0, 1800.0)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stock").value("AAPL"))
                .andExpect(jsonPath("$.pricePerShare").value(180.0))
                .andExpect(jsonPath("$.user.username").value("poster"))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    void createPostRejectsUnknownUsername() throws Exception {
        mockMvc.perform(post("/post/stock/")
                        .contentType(APPLICATION_JSON)
                        .content(createPostBody("ghost", "AAPL", 10.0, 1800.0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("unknown user")));
    }

    @Test
    void createPostRejectsMissingUsername() throws Exception {
        mockMvc.perform(post("/post/stock/")
                        .contentType(APPLICATION_JSON)
                        .content(createPostBody(null, "AAPL", 10.0, 1800.0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("username")));
    }

    @Test
    void createPostRejectsNonPositiveShares() throws Exception {
        createUser("sharesguy", true);

        mockMvc.perform(post("/post/stock/")
                        .contentType(APPLICATION_JSON)
                        .content(createPostBody("sharesguy", "AAPL", 0.0, 1800.0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("shares")));
    }

    // --- feed ---

    @Test
    void feedReturnsPublicPostsNewestFirst() throws Exception {
        WebUser pub = createUser("publicuser", true);
        postService.createPost(pub, LocalDate.parse("2026-07-01"), "AAPL", 1.0, 100.0);
        postService.createPost(pub, LocalDate.parse("2026-07-02"), "MSFT", 2.0, 200.0);

        mockMvc.perform(get("/post/feed/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].stock").value("MSFT"));
    }

    @Test
    void feedHidesPrivateAccountsPosts() throws Exception {
        WebUser priv = createUser("privateuser", false);
        postService.createPost(priv, LocalDate.parse("2026-07-01"), "TSLA", 1.0, 100.0);

        mockMvc.perform(get("/post/feed/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void feedIsEmptyListWhenNoPosts() throws Exception {
        mockMvc.perform(get("/post/feed/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // --- search ---

    @Test
    void searchReturnsMatchesForTicker() throws Exception {
        WebUser pub = createUser("searcher", true);
        postService.createPost(pub, LocalDate.parse("2026-07-01"), "NVDA", 1.0, 100.0);

        mockMvc.perform(post("/post/stock/search/").param("stockName", "nvda"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].stock").value("NVDA"));
    }

    @Test
    void searchReturnsEmptyListWhenNoMatches() throws Exception {
        mockMvc.perform(post("/post/stock/search/").param("stockName", "ZZZZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // --- delete ---

    @Test
    void ownerCanDeleteOwnPost() throws Exception {
        WebUser owner = createUser("owner", true);
        Post post = postService.createPost(owner, LocalDate.parse("2026-07-01"), "AAPL", 1.0, 100.0);

        mockMvc.perform(post("/post/delete/")
                        .param("postId", post.getId().toString())
                        .param("username", "owner"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/post/feed/"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void nonOwnerCannotDeletePost() throws Exception {
        WebUser owner = createUser("realowner", true);
        createUser("intruder", true);
        Post post = postService.createPost(owner, LocalDate.parse("2026-07-01"), "AAPL", 1.0, 100.0);

        mockMvc.perform(post("/post/delete/")
                        .param("postId", post.getId().toString())
                        .param("username", "intruder"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUnknownPostReturnsNotFound() throws Exception {
        createUser("deleter", true);

        mockMvc.perform(post("/post/delete/")
                        .param("postId", "999999")
                        .param("username", "deleter"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteWithoutParamsIsRejected() throws Exception {
        mockMvc.perform(post("/post/delete/"))
                .andExpect(status().isBadRequest());
    }
}
