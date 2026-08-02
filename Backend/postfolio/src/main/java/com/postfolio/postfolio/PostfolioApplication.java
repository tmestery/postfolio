package com.postfolio.postfolio;

import com.postfolio.postfolio.config.DotenvBootstrap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PostfolioApplication {

	public static void main(String[] args) {
		DotenvBootstrap.load();
		SpringApplication.run(PostfolioApplication.class, args);
	}

}