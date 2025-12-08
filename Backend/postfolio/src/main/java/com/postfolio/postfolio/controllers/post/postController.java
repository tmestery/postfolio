package com.postfolio.postfolio.controllers.post;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;

@RestController
@RequestMapping("/post")
public class postController {
    /**
     * POST http://localhost:8080/post/stock/
     *
     * @return a confirmation
     */
    @PostMapping("/stock/")
    public String agentStockInvesting(String username, LocalDate datePosted, String stockName, double shares, double investedAmount) {
        return "WebUser Post Created!";
    }
}