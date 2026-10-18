package com.sportvision.config;

import java.net.InetAddress;
import java.util.List;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    /** Local demonstration only: the existing login token is not authentication. */
    @Bean
    @Profile({"integration", "test", "local-demo"})
    public SecurityFilterChain demoFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()).cors(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/users",
                    "/api/products", "/api/products/**", "/api/categories", "/api/categories/**",
                    "/api/brands", "/api/brands/**", "/api/suppliers", "/api/suppliers/**",
                    "/api/orders", "/api/orders/**", "/api/favorites", "/api/favorites/**",
                    "/api/reviews", "/api/reviews/**", "/api/foods", "/api/foods/**",
                    "/api/stats/dashboard", "/api/ai/recommendations", "/api/ai/sales-forecast",
                    "/api/ai/coach-matching", "/api/ai/coaches",
                    "/api/training/exercises", "/api/training/exercises/**",
                    "/api/training/programs", "/api/training/programs/**",
                    "/api/training/workouts", "/api/training/workouts/**",
                    "/api/training/progress", "/api/training/progress/**",
                    "/api/training/schedules", "/api/training/schedules/**",
                    "/api/training/ai/options", "/api/training/ai/generate",
                    "/api/training/ai/programs", "/api/training/ai/programs/**").permitAll()
                .anyRequest().denyAll());
        return http.build();
    }

    @Bean
    @Profile("!integration & !test & !local-demo")
    public SecurityFilterChain closedFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.anyRequest().denyAll());
        return http.build();
    }

    @Bean
    @Profile({"integration", "test", "local-demo"})
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> demoLoopbackBinding() throws java.net.UnknownHostException {
        InetAddress loopback = InetAddress.getByName("127.0.0.1");
        return factory -> factory.setAddress(loopback);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200", "http://127.0.0.1:4200",
            "http://localhost:4201", "http://127.0.0.1:4201"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
