package com.sportvision.config;

import com.sportvision.dto.ApiResponse;
import com.sportvision.repository.UserRepository;
import com.sportvision.security.JwtAuthFilter;
import com.sportvision.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService, UserRepository userRepository, ObjectMapper mapper) throws Exception {
        http
            // API stateless authentifiée par jeton Bearer (pas de cookie de session => pas de CSRF)
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> writeError(mapper, res, 401, "Authentification requise"))
                .accessDeniedHandler((req, res, ex) -> writeError(mapper, res, 403, "Accès refusé"))
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.POST,
                        "/api/auth/login", "/api/auth/register",
                        "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                // Vitrine de la page d'accueil
                .requestMatchers(HttpMethod.GET, "/api/products/featured", "/api/categories").permitAll()

                // Rubrique administration
                .requestMatchers("/api/stats/**", "/api/suppliers/**", "/api/ai/sales-forecast").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/auth/users", "/api/orders", "/api/products/low-stock").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/orders/*/status").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/products/**", "/api/categories/**", "/api/brands/**", "/api/foods/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/products/**", "/api/categories/**", "/api/brands/**", "/api/foods/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/products/**", "/api/categories/**", "/api/brands/**", "/api/foods/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**", "/api/categories/**", "/api/brands/**", "/api/foods/**").hasRole("ADMIN")

                // Tout le reste exige une authentification
                .anyRequest().authenticated()
            )
            .addFilterBefore(new JwtAuthFilter(jwtService, userRepository), UsernamePasswordAuthenticationFilter.class);

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

    private void writeError(ObjectMapper mapper, jakarta.servlet.http.HttpServletResponse res, int status, String msg)
            throws java.io.IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        mapper.writeValue(res.getWriter(), ApiResponse.error(msg));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
