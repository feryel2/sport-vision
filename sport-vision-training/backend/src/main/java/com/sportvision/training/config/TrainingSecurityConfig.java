package com.sportvision.training.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** API de démonstration sans authentification : ne pas exposer publiquement. */
@Configuration
public class TrainingSecurityConfig {
    @Bean
    public SecurityFilterChain trainingSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/exercises", "/exercises/**", "/programs", "/programs/**", "/workouts", "/workouts/**", "/schedules", "/schedules/**", "/progress", "/progress/**", "/ai/**", "/error").permitAll()
                .anyRequest().denyAll());
        return http.build();
    }
}




