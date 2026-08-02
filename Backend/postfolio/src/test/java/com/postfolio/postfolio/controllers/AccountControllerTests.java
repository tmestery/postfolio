package com.postfolio.postfolio.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.postfolio.postfolio.models.user.UserRepository;
import com.postfolio.postfolio.models.user.WebUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccountControllerTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    private void createUser(String username) {
        WebUser user = new WebUser();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("encoded");
        user.setAccountStatus(true);
        userRepository.save(user);
    }

    private String statusBody(String username, Boolean accountPublic) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("accountPublic", accountPublic);
        return objectMapper.writeValueAsString(body);
    }

    @Test
    void ownerCanToggleAccountToPrivate() throws Exception {
        createUser("toggler");

        mockMvc.perform(post("/account/status/")
                        .contentType(APPLICATION_JSON)
                        .content(statusBody("toggler", false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountPublicStatus").value(false));

        mockMvc.perform(get("/account/status/").param("username", "toggler"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountPublicStatus").value(false));
    }

    @Test
    void setStatusRejectsUnknownUser() throws Exception {
        mockMvc.perform(post("/account/status/")
                        .contentType(APPLICATION_JSON)
                        .content(statusBody("ghost", false)))
                .andExpect(status().isNotFound());
    }

    @Test
    void setStatusRejectsMissingFields() throws Exception {
        mockMvc.perform(post("/account/status/")
                        .contentType(APPLICATION_JSON)
                        .content(statusBody("toggler", null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStatusRejectsUnknownUser() throws Exception {
        mockMvc.perform(get("/account/status/").param("username", "ghost"))
                .andExpect(status().isNotFound());
    }
}
