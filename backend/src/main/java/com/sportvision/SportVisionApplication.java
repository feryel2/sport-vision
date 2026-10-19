package com.sportvision;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class SportVisionApplication {

    public static void main(String[] args) {
        SpringApplication.run(SportVisionApplication.class, args);
        System.out.println("=================================================");
        System.out.println("  SPORT VISION - Backend démarré avec succès !   ");
        System.out.println("  API : http://localhost:8080/api                ");
        System.out.println("  H2 Console : http://localhost:8080/h2-console  ");
        System.out.println("=================================================");
    }
}
