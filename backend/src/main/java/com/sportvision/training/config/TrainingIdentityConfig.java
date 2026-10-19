package com.sportvision.training.config;

import com.sportvision.repository.UserRepository;
import com.sportvision.training.identity.TrainingCurrentUserProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;

@Configuration
public class TrainingIdentityConfig {
    @Bean
    @ConditionalOnMissingBean(TrainingCurrentUserProvider.class)
    TrainingCurrentUserProvider trainingCurrentUser(UserRepository users, Environment environment) {
        return () -> {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !(authentication instanceof AnonymousAuthenticationToken)) {
                // The JWT filter authenticates the email; the immutable database ID owns Training records.
                return users.findByEmail(authentication.getName()).filter(u -> u.isActive())
                    .map(u -> "training-user:" + u.getId()).orElse(null);
            }
            // Preserve local startup fixtures, never substitute a shared owner for an HTTP user.
            if (RequestContextHolder.getRequestAttributes() == null
                    && environment.acceptsProfiles(Profiles.of("local", "integration", "local-demo"))) {
                return "training-local-demo";
            }
            return null;
        };
    }
}
