package com.sportvision.training;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class TrainingApplication {
    public static void main(String[] args) {
        SpringApplication.run(TrainingApplication.class, args);
    }
}