package com.postfolio.postfolio.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String signupBody(String username, String email) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "username", username,
                "email", email,
                "password", "password123",
                "firstName", "Test",
                "lastName", "User"));
    }

    // --- signup ---

    @Test
    void signupSucceedsAndNeverReturnsPassword() throws Exception {
        mockMvc.perform(post("/credentials/signup/")
                        .contentType(APPLICATION_JSON)
                        .content(signupBody("alice", "alice@example.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.accountPublicStatus").value(true))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void signupRejectsDuplicateUsername() throws Exception {
        mockMvc.perform(post("/credentials/signup/")
                        .contentType(APPLICATION_JSON)
                        .content(signupBody("bob", "bob@example.com")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/credentials/signup/")
                        .contentType(APPLICATION_JSON)
                        .content(signupBody("bob", "other@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(containsString("username")));
    }

    @Test
    void signupRejectsDuplicateEmail() throws Exception {
        mockMvc.perform(post("/credentials/signup/")
                        .contentType(APPLICATION_JSON)
                        .content(signupBody("carol", "carol@example.com")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/credentials/signup/")
                        .contentType(APPLICATION_JSON)
                        .content(signupBody("carol2", "carol@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(containsString("email")));
    }

    @Test
    void signupRejectsMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/credentials/signup/")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "dave"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("required")));
    }

    // --- login ---

    @Test
    void loginReturnsAcceptedWithPlainTextUsername() throws Exception {
        mockMvc.perform(post("/credentials/signup/")
                        .contentType(APPLICATION_JSON)
                        .content(signupBody("erin", "erin@example.com")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/credentials/login/")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "erin", "password", "password123"))))
                .andExpect(status().isAccepted())
                .andExpect(content().string("erin"));
    }

    @Test
    void loginRejectsWrongPassword() throws Exception {
        mockMvc.perform(post("/credentials/signup/")
                        .contentType(APPLICATION_JSON)
                        .content(signupBody("frank", "frank@example.com")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/credentials/login/")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "frank", "password", "wrong-password"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginRejectsUnknownUser() throws Exception {
        mockMvc.perform(post("/credentials/login/")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "nobody", "password", "password123"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginRejectsMissingCredentials() throws Exception {
        mockMvc.perform(post("/credentials/login/")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "erin"))))
                .andExpect(status().isBadRequest());
    }
}
